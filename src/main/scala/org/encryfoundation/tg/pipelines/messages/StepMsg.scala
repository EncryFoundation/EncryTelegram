package org.encryfoundation.tg.pipelines.messages

import it.unisa.dia.gas.jpbc.Element
import org.encryfoundation.tg.userState.PrivateGroupChat

sealed trait StepMsg
object StepMsg {
  case class StartPipeline(pipelineName: String) extends StepMsg
  case class EndPipeline(pipelineName: String) extends StepMsg

  sealed trait GroupVerificationStepMsg extends StepMsg
  object GroupVerificationStepMsg {
    case class ProverFirstStepMsg(firstStep: Element,
                                  gTilda: Element,
                                  proverPublicKey1: Element,
                                  proverPublicKey2: Element,
                                  g1Gen: Element,
                                  g2Gen: Element,
                                  zRGen: Element) extends GroupVerificationStepMsg

    case class VerifierSecondStepMsg(verifierPubKey1: Element,
                                     secondStep: Element) extends GroupVerificationStepMsg

    case class ProverThirdStepMsg(thirdStep: Array[Byte],
                                  chatId: Long,
                                  name: String,
                                  privateGroupChat: PrivateGroupChat) extends GroupVerificationStepMsg
  }

  sealed trait WelcomeMsg extends StepMsg
  object WelcomeMsg {
    case class WelcomeResponseMsg(msgHash: Array[Byte]) extends WelcomeMsg
  }
}