package cc.aoeiuv020.irondb.impl

import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by AoEiuV020 on 2018.05.27-16:58:03.
 */
class GsonSerializerTest {
    private val serializer = GsonSerializer()

    @Test
    fun dataClassTest() {
        val student = Student(id = 10L, name = "AoEiuV020", age = 23)
        val json = """{"id":10,"name":"AoEiuV020","age":23}"""
        val string = serializer.serialize(student, object : TypeToken<Student>() {}.type)
        assertEquals(json, string)
        val obj: Student = serializer.deserialize(string, object : TypeToken<Student>() {}.type)
        assertEquals(student, obj)
    }

    @Test
    fun listTest() {
        val list = listOf(
                Student(id = 10L, name = "AoEiuV020", age = 23),
                Student(name = "name", age = -1)
        )
        val json = """[{"id":10,"name":"AoEiuV020","age":23},{"name":"name","age":-1}]"""
        val string = serializer.serialize(list, object : TypeToken<List<Student>>() {}.type)
        assertEquals(json, string)
        val obj: List<Student> = serializer.deserialize(string, object : TypeToken<List<Student>>() {}.type)
        assertEquals(list, obj)
    }

    data class Student(
            var id: Long? = null,
            val name: String,
            val age: Int
    )
}
