package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import ru.nsu.fit.conveyor.node.NewBaseNode
import ru.nsu.fit.conveyor.node.NodeInput
import ru.nsu.fit.conveyor.node.NodeOutput

@ExperimentalCoroutinesApi
class ConstantNode(description: String) : NewBaseNode(description) {
    init {
        inputs[0] = NodeInput(this, 0, Int::class)
        inputs[1] = NodeInput(this, 1, Int::class)
        outputs[0] = NodeOutput(this, 0, Int::class)
    }

    @Suppress("UNCHECKED_CAST")
    private val inputN: Channel<Int>
        get() = inputs[0]!!.channel as Channel<Int>

    @Suppress("UNCHECKED_CAST")
    private val inputCount: Channel<Int>
        get() = inputs[1]!!.channel as Channel<Int>


    @Suppress("UNCHECKED_CAST")
    private val output: Channel<Int>
        get() = outputs[0]!!.channel as Channel<Int>

    override suspend fun body() {
        log("Enter while")
        val n = inputN.receive()
        repeat(inputCount.receive()) {
            log("Send n=$n")
            output.send(n)
        }
        log("End of work")
    }

    companion object {
        const val DELAY = 1000L
    }
}