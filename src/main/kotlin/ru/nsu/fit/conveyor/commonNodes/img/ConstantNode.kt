package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import ru.nsu.fit.conveyor.node.BaseNode
import ru.nsu.fit.conveyor.node.NodeInput
import ru.nsu.fit.conveyor.node.NodeOutput

@ExperimentalCoroutinesApi
class ConstantNode : BaseNode("Constant node") {
    private inner class ConstantNodeContext : Context() {
        @Suppress("UNCHECKED_CAST")
        val inputN: Channel<Int>
            get() = inputs[0]!!.channel as Channel<Int>

        @Suppress("UNCHECKED_CAST")
        val inputCount: Channel<Int>
            get() = inputs[1]!!.channel as Channel<Int>


        @Suppress("UNCHECKED_CAST")
        val output: Channel<Int>
            get() = outputs[0]!!.channel as Channel<Int>

        init {
            inputs[0] = NodeInput(this@ConstantNode, 0, Int::class)
            inputs[1] = NodeInput(this@ConstantNode, 1, Int::class)
            outputs[0] = NodeOutput(this@ConstantNode, 0, Int::class)
        }
    }

    override fun initContext(withContext: Any?) {
        contexts[withContext] = ConstantNodeContext()
    }

    init {
        initContext(null)
    }

    override suspend fun body(context: Context) = with(context as ConstantNodeContext) {
        log("Enter while", context)
        val n = inputN.receive()
        repeat(inputCount.receive()) {
            log("Send n=$n", context)
            output.send(n)
        }
        log("End of work", context)
    }

    companion object {
        const val DELAY = 1000L
    }

    var dummy = "lalalal"
}