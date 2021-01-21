package ru.nsu.fit.conveyor.node

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
data class NodeInput<T : Any>(
    val node: BaseNode,
    val id: Int,
    val type: KClass<out T>,
    var channel: Channel<T> = Channel(Channel.BUFFERED),
    var connectedTo: BaseNode? = null
)

@ExperimentalCoroutinesApi
data class NodeOutput<T: Any>(
    val node: BaseNode,
    val id: Int,
    val type: KClass<out T>,
    var channel: Channel<T> = Channel(Channel.BUFFERED),
    var connectedTo: BaseNode? = null
)

@ExperimentalCoroutinesApi
abstract class BaseNode(
    val description: String
) {
    // FIXME replace map with list
    val outputs = mutableMapOf<Int, NodeOutput<Any>>()
    val inputs = mutableMapOf<Int, NodeInput<Any>>()

    suspend fun <T: Any> sendArg(id: Int, data: T) {
        inputs[id]?.let {
            if (data::class != it.type)
                error("[$description] Data type not match with input type: ${data::class} vs ${it.type}")
            it.channel.send(data)
        } ?: error("[$description] No input with id=$id")
    }

    suspend fun <T: Any> receiveArg(id: Int): T {
        return outputs[id]?.let {
            val data = it.channel.receive()
            @Suppress("UNCHECKED_CAST")
            data as? T ?: error("[$description] Data type not match with output type: ${data::class} vs ${it.type}")
        } ?: error("[$description] No output with id=$id")
    }

    // Может лучше запретить коннекты вне потока?
    fun connectOutputTo(outputId: Int, to: BaseNode, inputId: Int) {
        // TODO Check for channel not null
        outputs[outputId]?.let {
            val nodeInput = to.inputs[inputId]!!
            if (it.type != nodeInput.type)
                error("[$description] Type of input and output don't match: ${it.type} vs ${nodeInput.type}")
            it.channel = nodeInput.channel
            it.connectedTo = to
        }

    }

    protected open val isReady
        get() = inputs.all { (_, input) ->
            !input.channel.isEmpty
        }

    var isRunning: Boolean = false
        protected set

    open suspend fun tryRun(coroutineScope: CoroutineScope): Job = coroutineScope.launch {
        log("Try run")
        while (isReady) {
            log("Ready and run")
            isRunning = true
            body()
            tryRunOutputs(coroutineScope)
        }
        isRunning = false
    }

    private suspend fun tryRunOutputs(coroutineScope: CoroutineScope) {
        log("Try run outputs")
        outputs.forEach { (_, nodeOutput) ->
            log("Try run ${nodeOutput.connectedTo}")
            nodeOutput.connectedTo?.tryRun(coroutineScope)
        }
    }

    abstract suspend fun body()

    protected fun log(msg: String) = println("[${Thread.currentThread().name}][${this.description}] $msg")
}
