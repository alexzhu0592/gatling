/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.action.sse.fsm

import com.typesafe.scalalogging.StrictLogging
import io.gatling.commons.stats.{ KO, OK }
import io.gatling.core.action.Action
import io.gatling.core.session.Session

class SseClosingState(fsm: SseFsm, actionName: String, session: Session, next: Action, timestamp: Long) extends SseState(fsm) with StrictLogging {

  import fsm._

  override def onSseReceived(message: String, timestamp: Long): NextSseState = {
    logUnmatchedServerMessage(session)
    NextSseState(this)
  }

  override def onSseStreamClosed(closeStart: Long): NextSseState = {
    // server has acked closing
    logger.info("Socket closed")
    val newSession = logResponse(session, actionName, closeStart, timestamp, OK, None, None).remove(wsName)
    NextSseState(
      new SseClosedState(fsm),
      () => next ! newSession
    )
  }

  override def onSseStreamCrashed(t: Throwable, closeStart: Long): NextSseState = {
    logger.info("SSE stream crashed while waiting for socket close")
    // crash, close anyway
    val newSession = logResponse(session, actionName, closeStart, timestamp, KO, None, Some(t.getMessage)).markAsFailed.remove(wsName)
    NextSseState(
      new SseClosedState(fsm),
      () => next ! newSession
    )
  }
}
