package org.encryfoundation.tg.programs

import cats.Applicative
import cats.effect.{Concurrent, Sync, Timer}
import cats.effect.concurrent.{MVar, Ref}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import org.encryfoundation.tg.userState.UserState
import cats.implicits._
import javafx.scene.control.TextArea
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.{MessagePhoto, MessageText, MessageVideo}
import org.encryfoundation.tg.crypto.AESEncryption
import org.encryfoundation.tg.handlers.{AccumulatorHandler, ValueHandler}
import org.encryfoundation.tg.javaIntegration.JavaInterMsg
import org.encryfoundation.tg.javaIntegration.JavaInterMsg.SetActiveChat
import org.javaFX.model.JDialog
import scorex.crypto.encode.Base64

trait UIProgram[F[_]] {

  def run(): Stream[F, Unit]
}

object UIProgram {

  private class Live[F[_]: Concurrent: Timer: Logger](userStateRef: Ref[F, UserState[F]],
                                                      dialogAreaRef: MVar[F, TextArea],
                                                      jDialogRef: MVar[F, JDialog]) extends UIProgram[F] {

    def processLastMessage(msg: TdApi.Message, state: UserState[F]): String =
      msg.content match {
        case text: MessageText if state.privateGroups.contains(msg.chatId) =>
          val aes = AESEncryption(state.privateGroups(msg.chatId)._2.getBytes())
          state.users(msg.senderUserId).phoneNumber + ": " + aes.decrypt(Base64.decode(text.text.text).get).map(_.toChar).mkString
        case text: MessageText => state.users(msg.senderUserId).phoneNumber + ": " + text.text.text
        case _: MessagePhoto => state.users(msg.senderUserId).phoneNumber + ": " + "photo"
        case _: MessageVideo => state.users(msg.senderUserId).phoneNumber + ": " + "video"
        case _ => state.users(msg.senderUserId).phoneNumber + ": Unknown msg type"
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
              (msg: TdApi.Messages) => msg.messages.map(processLastMessage(_, state)).reverse.mkString("\n ").pure[F])
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
    }

    override def run: Stream[F, Unit] = (for {
      state <- Stream.eval(userStateRef.get)
      queue <- Stream.emit(state.javaState.get().msgsQueue)
      elem <- Stream.emit(queue.take())
      _ <- Stream.eval(Sync[F].delay(println(s"Elem: ${elem}")))
      _ <- Stream.eval(processMsg(elem))
    } yield ()).repeat
  }

  def apply[F[_]: Concurrent: Timer: Logger](userStateRef: Ref[F, UserState[F]]): F[UIProgram[F]] =
    for {
      dialogAreaMVar <- MVar.empty[F, TextArea]
      jDialogMVar <- MVar.empty[F, JDialog]
    } yield new Live(userStateRef, dialogAreaMVar, jDialogMVar)
}
