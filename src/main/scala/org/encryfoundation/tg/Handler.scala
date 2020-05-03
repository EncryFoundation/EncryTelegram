package org.encryfoundation.tg

import cats.effect.concurrent.Ref
import cats.effect.{ConcurrentEffect, Sync}
import cats.implicits._
import org.drinkless.tdlib.{Client, ResultHandler, TdApi}
import org.encryfoundation.tg.userState.UserState

import scala.io.StdIn

case class Handler[F[_]: ConcurrentEffect](userStateRef: Ref[F, UserState[F]]) extends ResultHandler[F] {

  /**
   * Callback called on result of query to TDLib or incoming update from TDLib.
   *
   * @param obj Result of query or update of type TdApi.Update about new events.
   */

  override def onResult(obj: TdApi.Object): F[Unit] = {
    //println(s"Obj: ${obj}")
    obj.getConstructor match {
      case TdApi.UpdateAuthorizationState.CONSTRUCTOR =>
        println("Auth state")
        println("Send")
        for {
          state <- userStateRef.get
          _ <- authHandler(obj.asInstanceOf[TdApi.UpdateAuthorizationState], state.client)
        } yield ()
      case TdApi.UpdateNewChat.CONSTRUCTOR =>
        val updateNewChat: TdApi.UpdateNewChat = obj.asInstanceOf[TdApi.UpdateNewChat]
        val chat: TdApi.Chat = updateNewChat.chat
        val order = chat.order
        chat.order = 0
        for {
            state <- userStateRef.get
            _ <- Sync[F].delay(println(s"Chat order: ${chat.order}. ${obj}"))
            _ <- userStateRef.update(_.copy(chatIds = state.chatIds + (chat.id -> chat)))
            _ <- setChatOrder(chat, order)
        } yield ()
        //println("Get updating chat!").pure[F]
      case any => ().pure[F]
        //println(s"Get unknown command in main handler: ${obj}").pure[F]
    }
  }

  def setChatOrder(chat: TdApi.Chat, order: Long): F[Unit] = {
    for {
      state <- userStateRef.get
      _ <- Sync[F].delay(println(s"Get chat order: ${order}"))
      _ <- if (chat.order != 0)
            userStateRef.update(_.copy(mainChatList = state.mainChatList.filter(_.order == chat.order)))
          else (()).pure[F]
      _ <- Sync[F].delay(chat.order = order)
      _ <- if (order != 0)
            userStateRef.update(_.copy(mainChatList = chat :: state.mainChatList))
          else (()).pure[F]
    } yield ()
  }

  def authHandler(authEvent: TdApi.UpdateAuthorizationState, client: Client[F]): F[Unit] = {
    println(s"kkk. Onj ${authEvent}. ${TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR}")
    authEvent.authorizationState match {
      case a: TdApi.AuthorizationStateWaitTdlibParameters =>
        println("trrr")
        val parameters = new TdApi.TdlibParameters
        parameters.databaseDirectory = "tdlib"
        parameters.useMessageDatabase = true
        parameters.useSecretChats = true
        parameters.apiId = 1257765
        parameters.apiHash = "8f6d710676dd9cb77c6c7fe24f09ee15"
        parameters.systemLanguageCode = "en"
        parameters.deviceModel = "Desktop"
        parameters.systemVersion = "Unknown"
        parameters.applicationVersion = "0.1"
        parameters.enableStorageOptimizer = true
        println("Here!")
        client.send(new TdApi.SetTdlibParameters(parameters), AuthRequestHandler[F]())
      case a: TdApi.AuthorizationStateWaitEncryptionKey =>
        println("abc")
        client.send(new TdApi.CheckDatabaseEncryptionKey(), AuthRequestHandler[F]())
      case a: TdApi.AuthorizationStateWaitPhoneNumber =>
        println("Enter phone number:")
        val phoneNumber = StdIn.readLine()
        client.send(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), AuthRequestHandler())
      case a: TdApi.AuthorizationStateWaitCode =>
        println("Enter code number:")
        val code = StdIn.readLine()
        client.send(new TdApi.CheckAuthenticationCode(code), AuthRequestHandler())
      case a: TdApi.AuthorizationStateWaitPassword =>
        println("Enter password")
        val pass = StdIn.readLine()
        client.send(new TdApi.CheckAuthenticationPassword(pass), AuthRequestHandler())
      case a: TdApi.AuthorizationStateReady =>
        println("Update state")
        userStateRef.update(_.copy(isAuth = true)).map(_ => ())
      case _ =>
        println(s"Got unknown event in auth. ${authEvent}").pure[F]
    }
  }
}

object Handler {
  def apply[F[_]: ConcurrentEffect](stateRef: Ref[F, UserState[F]],
                                    queueRef: Ref[F, List[TdApi.Object]]): F[Handler[F]] = {
    val handler = new Handler(stateRef)
    for {
      list <- queueRef.get
      _ <- Sync[F].delay(list)
      _ <- list.foreach(elem => Sync[F].delay(println(s"Send: ${elem.getConstructor}. ${list}")) >> handler.onResult(elem)).pure[F]
      handlerExp <- handler.pure[F]
    } yield handlerExp
  }
}
