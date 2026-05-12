package cc.aoeiuv020.irondb.impl

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinxSerializerTest {
    private val serializer = KotlinxSerializer()

    @Test
    fun dataClassTest() {
        val student = Student(id = 10L, name = "AoEiuV020", age = 23)
        val string = serializer.serialize(student, serializer())
        val obj: Student = serializer.deserialize(string, serializer())
        assertEquals(student, obj)
    }

    @Test
    fun listTest() {
        val list = listOf(
            Student(id = 10L, name = "AoEiuV020", age = 23),
            Student(name = "name", age = -1)
        )
        val string = serializer.serialize(list, serializer())
        val obj: List<Student> = serializer.deserialize(string, serializer())
        assertEquals(list, obj)
    }

    @Serializable
    data class Student(
        var id: Long? = null,
        val name: String,
        val age: Int
    )
}
