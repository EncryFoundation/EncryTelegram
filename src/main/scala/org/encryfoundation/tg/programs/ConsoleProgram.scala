package org.encryfoundation.tg.programs

import cats.{Applicative, Traverse}
import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, Sync, Timer}
import fs2.Stream
import io.chrisdavenport.log4cats.Logger
import org.drinkless.tdlib.Client
import org.encryfoundation.tg.commands.Command
import org.encryfoundation.tg.errors.TdError
import org.encryfoundation.tg.leveldb.Database
import org.encryfoundation.tg.services.{ClientService, PrivateConferenceService, UserStateService}
import org.encryfoundation.tg.userState.UserState
import tofu.Raise
import tofu.common.Console
import cats.implicits._

import scala.io.StdIn

trait ConsoleProgram[F[_]] {

  def run(): Stream[F, Unit]
}

object ConsoleProgram {

  private class Live[F[_]: Concurrent: Logger: Timer](clientService: ClientService[F],
                                                      userStateRef: Ref[F, UserState[F]],
                                                      confService: PrivateConferenceService[F],
                                                      userStateService: UserStateService[F],
                                                      db: Database[F])
                                                     (implicit err: Raise[F, TdError]) extends ConsoleProgram[F] {

    private val onlyForReg: F[Unit] =
      for {
        command <- Sync[F].delay {
          println("Write command. ('list')")
          val command = StdIn.readLine()
          println(s"Your command: ${command}.")
          command
        }
        _ <- Command.getCommands(userStateRef, db)(confService, userStateService, clientService).find(_.name == command.split(" ").head)
          .traverse(_.run(command.split(" ").tail.toList))
        _ <- onlyForReg
      } yield ()

    private val regComm: F[Unit] =
      userStateRef.get.flatMap(state =>
        if (state.isAuth) onlyForReg
        else regComm
      )

    override def run: Stream[F, Unit] = Stream.eval(regComm)
  }

  def apply[F[_]: Concurrent: Logger: Timer:
                  Raise[*[_], TdError]: Console](clientService: ClientService[F],
                                                 userStateRef: Ref[F, UserState[F]],
                                                 confService: PrivateConferenceService[F],
                                                 userStateService: UserStateService[F],
                                                 db: Database[F]): F[ConsoleProgram[F]] =
    Applicative[F].pure(new Live(clientService, userStateRef, confService, userStateService, db))
}
