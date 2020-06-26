package org.encryfoundation.tg.programs

import cats.Applicative
import cats.effect.{Concurrent, Sync, Timer}
import cats.effect.concurrent.{MVar, Ref}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import org.encryfoundation.tg.userState.UserState
import cats.implicits._
import javafx.scene.control.TextArea
import org.drinkless.tdlib.{Client, ClientUtils, TdApi}
import org.drinkless.tdlib.TdApi.{MessagePhoto, MessageText, MessageVideo}
import org.encryfoundation.tg.crypto.AESEncryption
import org.encryfoundation.tg.handlers.{AccumulatorHandler, PrivateGroupChatCreationHandler, ValueHandler}
import org.encryfoundation.tg.javaIntegration.JavaInterMsg
import org.encryfoundation.tg.javaIntegration.JavaInterMsg.{CreateCommunityJava, CreatePrivateGroupChat, SendToChat, SetActiveChat}
import org.encryfoundation.tg.community.PrivateCommunity
import org.encryfoundation.tg.leveldb.Database
import org.encryfoundation.tg.services.{PrivateConferenceService, UserStateService}
import org.javaFX.model.JDialog
import scorex.crypto.encode.Base64

import collection.JavaConverters._
import scala.util.Try

trait UIProgram[F[_]] {

  def run(): Stream[F, Unit]
}

object UIProgram {

  private class Live[F[_]: Concurrent: Timer: Logger](userStateRef: Ref[F, UserState[F]],
                                                      privateConfService: PrivateConferenceService[F],
                                                      userStateService: UserStateService[F],
                                                      client: Client[F],
                                                      dialogAreaRef: MVar[F, TextArea],
                                                      jDialogRef: MVar[F, JDialog]) extends UIProgram[F] {

    def processLastMessage(msg: TdApi.Message, state: UserState[F]): F[String] =
      userStateService.getPrivateGroupChat(msg.chatId).map {
        case Some(privateGroupChat) =>
          msg.content match {
            case text: MessageText =>
              val aes = AESEncryption(privateGroupChat.password.getBytes())
              Try(
                state.users.get(msg.senderUserId)
                  .map(_.phoneNumber)
                  .getOrElse("Unknown sender") + ": " +
                  aes.decrypt(Base64.decode(text.text.text).get).map(_.toChar).mkString).getOrElse("Unkown msg")
            case _ => state.users.get(msg.senderUserId).map(_.phoneNumber).getOrElse("Unknown sender") + ": Unknown msg type"
          }
        case None =>
          msg.content match {
            case text: MessageText => state.users.get(msg.senderUserId).map(_.phoneNumber).getOrElse("Unknown sender") + ": " + text.text.text
            case _: MessagePhoto => state.users.get(msg.senderUserId).map(_.phoneNumber).getOrElse("Unknown sender") + ": " + "photo"
            case _: MessageVideo => state.users.get(msg.senderUserId).map(_.phoneNumber).getOrElse("Unknown sender") + ": " + "video"
            case _ => state.users.get(msg.senderUserId).map(_.phoneNumber).getOrElse("Unknown sender") + ": Unknown msg type"
          }
      }

    def processMsg(msg: JavaInterMsg): F[Unit] = msg match {
      case _@SetActiveChat(chatId) =>
        for {
          state <- userStateRef.get
          javaState <- state.javaState.get().pure[F]
          msgsMVar <- MVar.empty[F, String]
          _ <- state.client.send(
            new TdApi.GetChatHistory(chatId, 0, 0, 20, false),
            ValueHandler(
              userStateRef,
              msgsMVar,
              (msg: TdApi.Messages) =>
                msg.messages.toList.traverse(processLastMessage(_, state)).map(_.reverse.mkString("\n ")))
          )
          _ <- userStateRef.update(_.copy(activeChat = chatId))
          msgs <- msgsMVar.read
          _ <- Sync[F].delay {
            javaState.activeDialog.setContent(new StringBuffer())
            val localDialogHistory = javaState.activeDialog.getContent
            localDialogHistory.append(msgs + "\n")
            javaState.activeDialogArea.setText(localDialogHistory.toString)
            javaState.activeDialog.setContent(localDialogHistory)
          }
        } yield ()
      case _@SendToChat(msg) =>
        userStateRef.get.flatMap( state => ClientUtils.sendMsg(state.chatList.find(_.id == state.activeChat).get, msg, userStateRef))
      case _@CreateCommunityJava(name, usersJava) =>
        privateConfService.createConference(name, "me" :: usersJava.asScala.toList)
      case _@CreatePrivateGroupChat(name) =>
        for {
          state <- userStateRef.get
          communityBytes <- state.db.get(PrivateConferenceService.confInfo(name))
          _ <- communityBytes match {
            case Some(bytes) =>
              val community = PrivateCommunity.parseBytes(bytes).get
              createGroup(
                name + "Chat",
                name,
                "1234",
                community.users.tail.map(_.userTelegramLogin)
              )
            case None => Sync[F].delay(println("Got nothing"))
          }
        } yield ()
    }

    private def createGroup(groupname: String,
                            conferenceName: String,
                            password: String,
                            users: List[String]): F[Unit] = {
      for {
        state <- userStateRef.get
        userIds <- Concurrent[F].delay(users.flatMap(
          username => state.users.find(userInfo => userInfo._2.username == username || userInfo._2.phoneNumber == username)
        ))
        _ <- Logger[F].info(s"Create private group chat for conference ${conferenceName} with next group: ${groupname} " +
          s"and users(${users}):")
        confInfo <- privateConfService.findConf(conferenceName)

        _ <- client.send(
          new TdApi.CreateNewBasicGroupChat(userIds.map(_._1).toArray, groupname),
          PrivateGroupChatCreationHandler[F](
            userStateRef,
            client,
            confInfo,
            groupname,
            userIds.map(_._2),
            confInfo.users.head.userTelegramLogin,
            password
          )(privateConfService, userStateService)
        )
        _ <- state.db.put(Database.privateGroupChatsKey, groupname.getBytes())
        _ <- state.db.put(groupname.getBytes(), password.getBytes())
      } yield ()
    }

    override def run: Stream[F, Unit] = (for {
      state <- Stream.eval(userStateRef.get)
      queue <- Stream.emit(state.javaState.get().msgsQueue)
      elem <- Stream.emit(queue.take())
      _ <- Stream.eval(processMsg(elem))
    } yield ()).repeat
  }

  def apply[F[_]: Concurrent: Timer: Logger](userStateRef: Ref[F, UserState[F]],
                                             privateConfService: PrivateConferenceService[F],
                                             userStateService: UserStateService[F],
                                             client: Client[F]): F[UIProgram[F]] =
    for {
      dialogAreaMVar <- MVar.empty[F, TextArea]
      jDialogMVar <- MVar.empty[F, JDialog]
    } yield new Live(userStateRef, privateConfService, userStateService, client, dialogAreaMVar, jDialogMVar)
}
