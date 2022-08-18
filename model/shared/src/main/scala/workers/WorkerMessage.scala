// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package workers

import algebra.instances.array.*
import boopickle.DefaultBasic.*
import lucuma.utils.*

import java.util.UUID

private object WorkerProcessId extends NewType[UUID]
private type WorkerProcessId = WorkerProcessId.Type

private object Pickled extends NewType[Array[Byte]]
private type Pickled = Pickled.Type

// Low-level protocol messages, not to be used by clients or servers
private object WorkerMessage {
  sealed trait FromClient
  object FromClient {
    final case class Start(id: WorkerProcessId, payload: Pickled) extends FromClient
    final case class End(id: WorkerProcessId)                     extends FromClient
  }

  sealed trait FromServer
  object FromServer {
    final case class Data(id: WorkerProcessId, payload: Pickled)        extends FromServer
    final case class Complete(id: WorkerProcessId)                      extends FromServer
    final case class Error(id: WorkerProcessId, error: WorkerException) extends FromServer
  }

  private given Pickler[WorkerProcessId] = transformPickler(WorkerProcessId.apply)(_.value)

  private given Pickler[Pickled] = transformPickler(Pickled.apply)(_.value)

  private given Pickler[FromClient.Start] = generatePickler

  private given Pickler[FromClient.End] = generatePickler

  given Pickler[FromClient] = generatePickler

  private given Pickler[FromServer.Data] = generatePickler

  private given Pickler[FromServer.Complete] = generatePickler

  private given Pickler[FromServer.Error] = generatePickler

  given Pickler[FromServer] = generatePickler
}