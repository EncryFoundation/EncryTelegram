package org.encryfoundation.tg.services

import cats.Applicative
import cats.data.OptionT
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory
import org.encryfoundation.sectionSeven.SectionSeven
import org.encryfoundation.tg.community.{CommunityUser, PrivateCommunity}
import org.encryfoundation.tg.leveldb.Database
import org.encryfoundation.tg.userState.{ConferencesNames, UserState}
import org.javaFX.model.JLocalCommunity
import scorex.crypto.hash.Blake2b256

trait PrivateConferenceService[F[_]] {
  def createConference(name: String, users: List[String]): F[Unit]
  def deleteConference(name: String): F[Unit]
  def addUserToConf(conf: String, userName: String): F[Unit]
  def sendInvite(conf: String, userName: String): F[Unit]
  def findConf(conf: String): F[PrivateCommunity]
  def saveMyConfCredentials(conf: String, userInfo: CommunityUser): F[Unit]
  def deleteUserFromConf(conf: String, userName: String): F[Unit]
  def getConfs: F[List[PrivateCommunity]]
}

object PrivateConferenceService {

  val conferencesKey = Blake2b256("Conferences")

  private class Live[F[_]: Sync: Logger](db: Database[F],
                                         userStateRef: Ref[F, UserState[F]]) extends PrivateConferenceService[F] {

    override def createConference(name: String, users: List[String]): F[Unit] =
      for {
        state <- userStateRef.get
        jState <- Sync[F].delay(state.javaState.get())
        pairing <- Sync[F].delay(PairingFactory.getPairing("properties/a.properties"))
        generatorG1 <- Sync[F].delay(pairing.getG1.newRandomElement().getImmutable)
        generatorG2 <- Sync[F].delay(generatorG1.getImmutable)
        generatorZr <- Sync[F].delay(pairing.getZr.newRandomElement().getImmutable)
        sevSec <- Sync[F].delay(SectionSeven(generatorG1, generatorG2, pairing))
        usersInfo <- Sync[F].delay(sevSec.genElems(users.length))
        usersIds <- users.zip(usersInfo._1).map { case (userLogin, userInfo) =>
          CommunityUser(userLogin, userInfo)
        }.pure[F]
        community <- PrivateCommunity(name, usersIds, generatorG1, generatorG2, generatorZr, usersInfo._2).pure[F]
        _ <- Logger[F].info(s"Create private community with name: ${name}. com: ${jState.communities}. And users: ${usersIds.map(_.userTelegramLogin)}")
        _ <- Sync[F].delay(jState.communities.add(new JLocalCommunity(name, community.users.length)))
        _ <- db.put(
          conferencesKey,
          ConferencesNames.toBytes(ConferencesNames(state.privateCommunities.map(_.name) :+ name))
        )
        _ <- db.put(confInfo(name), PrivateCommunity.toBytes(community))
      } yield ()

    override def addUserToConf(conf: String, userName: String): F[Unit] = ???

    override def deleteUserFromConf(conf: String, userName: String): F[Unit] = ???

    override def getConfs: F[List[PrivateCommunity]] = (for {
      possibleConfs <- OptionT(db.get(conferencesKey))
      confNames <- OptionT.fromOption[F](ConferencesNames.parseBytes(possibleConfs).toOption)
      privateConfs <- OptionT.liftF[F, List[PrivateCommunity]](confNames.conferences.traverse(recoverConf).map(_.flatten))
    } yield privateConfs).getOrElse(List.empty)

    override def saveMyConfCredentials(conf: String,
                                       userInfo: CommunityUser): F[Unit] = ???

    override def sendInvite(conf: String, userName: String): F[Unit] = ???

    override def findConf(conf: String): F[PrivateCommunity] =
      getConfs.map(_.find(_.name == conf).get)

    private def recoverConf(confName: String): F[Option[PrivateCommunity]] = (for {
      possibleBytes <- OptionT(db.get(confInfo(confName)))
      possibleConf <- OptionT.fromOption[F](PrivateCommunity.parseBytes(possibleBytes).toOption)
    } yield possibleConf).value

    override def deleteConference(name: String): F[Unit] = db.remove(confInfo(name))
  }

  def confInfo(confName: String) = Blake2b256(s"ConfInfo${confName}")

  def apply[F[_]: Sync: Logger](db: Database[F], userStateRef: Ref[F, UserState[F]]): F[PrivateConferenceService[F]] =
    Sync[F].delay(new Live[F](db, userStateRef))
}
