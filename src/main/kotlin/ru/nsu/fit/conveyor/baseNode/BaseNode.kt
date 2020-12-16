package ru.nsu.fit.conveyor.baseNode

typealias DataById = Map<Int, List<Any>>

abstract class BaseNode(
    val description: String
) {
    // FIXME нужна ли какая-то оченедь входных данных?

    protected var inputTypes: MutableMap<Int, Class<*>> = mutableMapOf()
    protected var outputTypes: MutableMap<Int, Class<*>> = mutableMapOf()

    fun addInput(id: Int, type: Class<*>) {
        inputTypes[id] = type
    }

    fun addOutput(id: Int, type: Class<*>) {
        outputTypes[id] = type
    }

    fun getInputType(id: Int): Class<*>? = inputTypes[id]

    fun getOutputType(id: Int): Class<*>? = outputTypes[id]

    abstract suspend fun run(inputs: DataById): DataById
}