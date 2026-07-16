package com.qtpie.simplepuzzle.learning

import com.qtpie.simplepuzzle.core.learning.AttemptId
import com.qtpie.simplepuzzle.core.learning.LearningIdSource
import com.qtpie.simplepuzzle.core.learning.SessionId
import java.util.UUID

class UuidLearningIdSource : LearningIdSource {
    override fun nextAttemptId(): AttemptId = AttemptId("attempt-${UUID.randomUUID()}")

    override fun nextSessionId(): SessionId = SessionId("session-${UUID.randomUUID()}")
}
