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
    val contexts = mutableMapOf<Any?, Context>()

    // FIXME replace map with list

    abstract inner class Context {
        val outputs = mutableMapOf<Int, NodeOutput<Any>>()
        val inputs = mutableMapOf<Int, NodeInput<Any>>()
    }

    abstract fun initContext(withContext: Any?)

    suspend fun <T: Any> sendArg(id: Int, data: T, withContext: Any? = null) {
        contexts[withContext]!!.inputs[id]?.let {
            if (data::class != it.type)
                error("[$description] Data type not match with input type: ${data::class} vs ${it.type}")
            it.channel.send(data)
        } ?: error("[$description] No input with id=$id")
    }

    suspend fun <T: Any> receiveArg(id: Int, withContext: Any? = null): T {
        return contexts[withContext]!!.outputs[id]?.let {
            val data = it.channel.receive()
            @Suppress("UNCHECKED_CAST")
            data as? T ?: error("[$description] Data type not match with output type: ${data::class} vs ${it.type}")
        } ?: error("[$description] No output with id=$id")
    }

    // Может лучше запретить коннекты вне потока?
    fun connectOutputTo(outputId: Int, to: BaseNode, inputId: Int, withContext: Any? = null) {
        // TODO Check for channel not null
        contexts[withContext]!!.outputs[outputId]?.let {
            val nodeInput = to.contexts[withContext]!!.inputs[inputId]!!
            if (it.type != nodeInput.type)
                error("[$description] Type of input and output don't match: ${it.type} vs ${nodeInput.type}")
            it.channel = nodeInput.channel
            it.connectedTo = to
        }

    }

    open fun isReady(withContext: Any?) =
        contexts[withContext]!!.inputs.all { (_, input) ->
            !input.channel.isEmpty
        }

    var isRunning: Boolean = false
        protected set

    open suspend fun tryRun(coroutineScope: CoroutineScope, withContext: Any? = null): Job = coroutineScope.launch {
        log("Try run", contexts[withContext]!!)
        while (isReady(withContext)) {
            log("Ready and run", contexts[withContext]!!)
            isRunning = true
            body(contexts[withContext]!!)
            tryRunOutputs(coroutineScope, withContext)
        }
        isRunning = false
    }

    private suspend fun tryRunOutputs(coroutineScope: CoroutineScope, withContext: Any?) {
        log("Try run outputs", contexts[withContext]!!)
        contexts[withContext]!!.outputs.forEach { (_, nodeOutput) ->
            log("Try run ${nodeOutput.connectedTo}", contexts[withContext]!!)
            nodeOutput.connectedTo?.tryRun(coroutineScope, withContext)
        }
    }

    abstract suspend fun body(context: Context)

    protected fun log(msg: String, context: Context) = println("[${Thread.currentThread().name}][${this.description}] $msg")
}
