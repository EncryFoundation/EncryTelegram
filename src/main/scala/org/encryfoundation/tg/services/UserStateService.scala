package org.encryfoundation.tg.services

import cats.effect.Sync
import cats.effect.concurrent.Ref
import org.drinkless.tdlib.TdApi
import org.encryfoundation.tg.leveldb.Database
import org.encryfoundation.tg.pipelines.Pipeline
import org.encryfoundation.tg.userState.{PrivateGroupChat, PrivateGroupChats, UserState}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import collection.JavaConverters._

trait UserStateService[F[_]] {
  //basic chats ops
  def addChat(chat: TdApi.Chat): F[Unit]
  def updateChatOrder(chat: TdApi.Chat, newOrder: Long): F[Unit]
  def getChatById(chatId: Long): F[Option[TdApi.Chat]]
  //secret chats
  def addSecretChat(chat: TdApi.SecretChat): F[Unit]
  def removeSecretChat(chat: TdApi.SecretChat): F[Unit]
  //private group chats
  def getPrivateGroupChat(chatId: Long): F[Option[PrivateGroupChat]]
  def persistPrivateGroupChat(privateGroupChat: PrivateGroupChat): F[Unit]
  //groups
  def updateBasicGroup(basicGroup: TdApi.UpdateBasicGroup): F[Unit]
  def updateSuperGroup(superGroup: TdApi.UpdateSupergroup): F[Unit]
  //pipeline chats
  def addPipelineChat(chat: TdApi.Chat, newPipeline: Pipeline[F]): F[Unit]
  def updatePipelineChat(secretChatId: Long, newPipeline: Pipeline[F]): F[Unit]
  def addPendingPipelineChat(chat: TdApi.Chat, newPipeline: Pipeline[F]): F[Unit]
  def getPipelineChatIdBySecChat(secretChatId: Int): F[Option[Long]]
  def getPipeline(chatId: Long): F[Option[Pipeline[F]]]
  //users
  def updateUser(user: TdApi.User): F[Unit]
}

object UserStateService {

  private final class Live[F[_]: Sync: Logger](userState: Ref[F, UserState[F]],
                                               db: Database[F]) extends UserStateService[F] {

    override def addChat(chat: TdApi.Chat): F[Unit] =
      userState.update { prevState =>
        if (!prevState.pendingSecretChatsForInvite.exists(_._2._1.id == chat.id) &&
          !prevState.pipelineSecretChats.contains(chat.id))
          prevState.copy(chatIds = prevState.chatIds + (chat.id -> chat))
        else prevState
      }

    override def getChatById(chatId: Long): F[Option[TdApi.Chat]] =
      userState.get.map(_.chatIds.find(_._2.id == chatId).map(_._2))

    override def addSecretChat(chat: TdApi.SecretChat): F[Unit] =
      userState.update { prevState =>
        if (!prevState.pendingSecretChatsForInvite.exists(_._2._1.id == chat.id) &&
          !prevState.pipelineSecretChats.contains(chat.id))
          prevState.copy(secretChats = prevState.secretChats + (chat.id -> chat))
        else prevState
      }

    override def removeSecretChat(chat: TdApi.SecretChat): F[Unit] =
      userState.update { prevState =>
        prevState.copy(secretChats = prevState.secretChats - chat.id)
      }

    override def persistPrivateGroupChat(privateGroupChat: PrivateGroupChat): F[Unit] =
      userState.updateAndGet(prevState =>
        prevState.copy(
          privateGroups = prevState.privateGroups + privateGroupChat
        )
      ).flatMap(newState =>
        db.put(UserState.privateChatsKey, PrivateGroupChats.toBytes(newState.privateGroups.toList))
      ) >> Logger[F].info(s"Persist private group chat: ${privateGroupChat}")

    override def addPipelineChat(chat: TdApi.Chat, pipeline: Pipeline[F]): F[Unit] =
      userState.update(prevState =>
        prevState.copy(
          pipelineSecretChats = prevState.pipelineSecretChats + (chat.id -> pipeline)
        )
      )

    override def updatePipelineChat(chatId: Long, newPipeline: Pipeline[F]): F[Unit] = for {
      state <- userState.get
      pending <- state.pendingSecretChatsForInvite.get(chatId).pure[F]
      _ <- if (pending.nonEmpty) userState.update { prevState =>
        prevState.copy(
          pendingSecretChatsForInvite = prevState.pendingSecretChatsForInvite - chatId,
          pipelineSecretChats = state.pipelineSecretChats + (pending.get._1.id -> newPipeline)
        )
      } else userState.update { prevState =>
        prevState.copy(
          pipelineSecretChats = state.pipelineSecretChats + (chatId -> newPipeline)
        )
      }
    } yield ()

    override def getPipeline(secretChatId: Long): F[Option[Pipeline[F]]] = for {
      state <- userState.get
      _ <- Logger[F].info(s"pipelines: ${state.pendingSecretChatsForInvite}")
      pendingPipeLine <- state.pendingSecretChatsForInvite.find(_._1 == secretChatId).map(_._2._2).pure[F]
      privatePipeline <- userState.get.map(_.pipelineSecretChats.get(secretChatId))
    } yield pendingPipeLine.orElse(privatePipeline)

    override def getPrivateGroupChat(chatId: Long): F[Option[PrivateGroupChat]] =
      userState.get.map(_.privateGroups.find(_.chatId == chatId))

    override def addPendingPipelineChat(chat: TdApi.Chat,
                                        newPipeline: Pipeline[F]): F[Unit] = chat.`type` match {
      case secret: TdApi.ChatTypeSecret =>
        Logger[F].info(s"Add chat with id: ${chat.id} and secret chat id: ${secret.secretChatId}") >>
        userState.update { prevState =>
          prevState.copy(
            pendingSecretChatsForInvite = prevState.pendingSecretChatsForInvite +
              (chat.id -> (chat, newPipeline)),
            pipelineSecretChatInfo = prevState.pipelineSecretChatInfo + (secret.secretChatId -> chat.id)
          )
        }
      case _ =>  ().pure[F]
    }

    override def updateUser(user: TdApi.User): F[Unit] = for {
      _ <- userState.update { prevState =>
        val newUsers = prevState.javaState.get().getUsers
        newUsers.put(user.id, user)
        prevState.javaState.get().setUsers(newUsers)
        prevState.copy(users = prevState.users + (user.id -> user))
      }
    } yield ()

    override def updateChatOrder(chat: TdApi.Chat, newOrder: Long): F[Unit] =
      userState.get.flatMap { state =>
        if (!state.pendingSecretChatsForInvite.exists(_._2._1.id == chat.id) &&
          !state.pipelineSecretChats.contains(chat.id))
          for {
            _ <- userState.update(prevState =>
              if (prevState.chatList.length < 20) prevState.copy(
                chatList = prevState.mainChatList.takeRight(20).values.toList
              ) else prevState
            )
            _ <- Sync[F].delay(chat.order = newOrder)
            _ <- userState.update(prevState =>
              if (newOrder != 0) {
                prevState.javaState.get().setChatList(
                  (chat :: prevState.chatList.filterNot(_.id == chat.id)).sortBy(_.order).takeRight(20).reverse.asJava
                )
                prevState.copy(
                  chatList = (chat :: prevState.chatList.filterNot(_.id == chat.id)).sortBy(_.order).takeRight(20),
                  mainChatList = (prevState.mainChatList.filterNot(_._2.id == chat.id) + (newOrder -> chat))
                )
              } else prevState
            )
          } yield ()
        else ().pure[F]
      }

    override def updateBasicGroup(basicGroup: TdApi.UpdateBasicGroup): F[Unit] =
      Logger[F].info("Invoke updateBasiGroup") >>
      userState.update(prevState =>
        prevState.copy(basicGroups = prevState.basicGroups + (basicGroup.basicGroup.id -> basicGroup.basicGroup))
      )

    override def updateSuperGroup(superGroup: TdApi.UpdateSupergroup): F[Unit] =
      userState.update(prevState =>
        prevState.copy(superGroups = prevState.superGroups + (superGroup.supergroup.id -> superGroup.supergroup))
      )

    override def getPipelineChatIdBySecChat(secretChatId: Int): F[Option[Long]] =
      Logger[F].info("Invoke getPipelineChatIdBySecChat") >>
      userState.get.map(_.pipelineSecretChatInfo.get(secretChatId))
  }

  def apply[F[_]: Sync: Logger](userState: Ref[F, UserState[F]],
                               db: Database[F]): F[UserStateService[F]] = Sync[F].delay(new Live[F](userState, db))
}
