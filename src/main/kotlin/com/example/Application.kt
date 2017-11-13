package com.example

import org.mapdb.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.*

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

@Controller
class Signup(@Autowired val map: HTreeMap<String, Model>) {

    @PostMapping(value = "/signup", consumes = arrayOf("application/x-www-form-urlencoded;charset=UTF-8"))
    fun signup(@RequestParam("firstname") firstname: String,
               @RequestParam("lastname") lastname: String,
               @RequestParam("company") company: String,
               @RequestParam("title") title: String,
               @RequestParam("department") department: String,
               @RequestParam("email") email: String,
               @RequestParam("city") city: String,
               @RequestParam("country") country: String,
               @RequestParam("phonenumber") phonenumber: String): String {

        val uuid = UUID.randomUUID().toString()
        val model = Model(firstname, lastname, company, title, department, email, city, country, phonenumber)
        map.put(uuid, model)

        return "redirect:/success.html"
    }

    @RequestMapping(value = "/records/{uuid}")
    fun loadRecord(@PathVariable("uuid") uuid: String, model: org.springframework.ui.Model): String {
        model.addAttribute("record", map.get(uuid))
        map.get(uuid)?.run {
            model.addAttribute("uuid", uuid)
            model.addAttribute("firstname", firstname)
            model.addAttribute("lastname", lastname)
            model.addAttribute("company", company)
            model.addAttribute("title", title)
            model.addAttribute("department", department)
            model.addAttribute("email", email)
            model.addAttribute("city", city)
            model.addAttribute("country", country)
            model.addAttribute("phonenumber", phonenumber)
            model.addAttribute("comment", comment)
        } ?: throw RuntimeException("Record '$uuid' not found")
        return "showrecord"
    }

    @PostMapping(value = "/records/{uuid}", consumes = arrayOf("application/x-www-form-urlencoded;charset=UTF-8"))
    fun update(@PathVariable("uuid") uuid: String,
               @RequestParam("firstname") firstname: String,
               @RequestParam("lastname") lastname: String,
               @RequestParam("company") company: String,
               @RequestParam("title") title: String,
               @RequestParam("department") department: String,
               @RequestParam("email") email: String,
               @RequestParam("city") city: String,
               @RequestParam("country") country: String,
               @RequestParam("phonenumber") phonenumber: String,
               @RequestParam("comment") comment: String): String {

        val model = Model(firstname, lastname, company, title, department, email, city, country, phonenumber, comment)
        map.put(uuid, model)

        return "redirect:/records"
    }

    @RequestMapping(value = "/records")
    fun list(model: org.springframework.ui.Model): String {
        model.addAttribute("count", map.size)
        model.addAttribute("records", map.entries.map { Record(it.key!!, it.value!!.firstname, it.value!!.lastname, it.value!!.company) })
        return "selection"
    }

    @GetMapping(value = "/raffle")
    fun raffle(model: org.springframework.ui.Model): String {
        val entries = map.entries.toTypedArray()
        val size = entries.size
        val luckyNumberSleven = Random().nextInt(size)
        val winner = entries[luckyNumberSleven]

        model.addAttribute("name", winner.value?.run { firstname + " " + lastname })
        model.addAttribute("email", winner.value!!.email)
        model.addAttribute("uuid", winner.key)

        return "raffle"
    }

    @GetMapping(value = "/raffle/{uuid}")
    fun raffle_matrix(@PathVariable("uuid") uuid: String, model: org.springframework.ui.Model): String {
        val winner = map.get(uuid)
        model.addAttribute("name", winner!!.run { firstname + " " + lastname })
        model.addAttribute("email", winner!!.email)
        model.addAttribute("uuid", uuid)
        return "raffle"
    }
}

@RestController
class DataController(@Autowired val map: HTreeMap<String, Model>) {
    @GetMapping(value = "/registrations", produces = arrayOf("application/json"))
    fun list(): MutableSet<MutableMap.MutableEntry<String?, Model?>> = map.entries

    @GetMapping(value = "/export.csv", produces = arrayOf("text/csv"))
    fun export(): String = map.entries.filter { it.value != null }.map { Pair(it.key, it.value!!) }
            .map { arrayOf(it.first).plus(it.second.asArray()).csvjoin() }.joinToString("\n")

    @RequestMapping(value = "/records/{uuid}", produces = arrayOf("application/json"), method = arrayOf(RequestMethod.DELETE))
    fun delete(@PathVariable("uuid") uuid: String): String {
        map.remove(uuid)
        return "{}"
    }
}

@Component
class Database {
    @Bean
    fun mapdb() = DBMaker.fileDB("raffle.mapdb").closeOnJvmShutdown().make()

    @Bean
    fun map(@Autowired db: DB) = db.hashMap("raffle", Serializer.STRING, ModelSerializer()).createOrOpen()
}

data class Record(val uuid: String, val firstname: String, val lastname: String, val company: String)

data class Model(val firstname: String,
                 val lastname: String,
                 val company: String,
                 val title: String,
                 val department: String,
                 val email: String,
                 val city: String,
                 val country: String,
                 val phonenumber: String,
                 val comment: String = "") {

    fun asArray(): Array<String?> = arrayOf(firstname, lastname, company, title, department, email, city,
            country, if (phonenumber.isEmpty()) "" else "=\"\"" + phonenumber + "\"\"", comment)
}

class ModelSerializer : Serializer<Model> {
    override fun serialize(out: DataOutput2, value: Model) = with(out) {
        writeUTF(value.firstname)
        writeUTF(value.lastname)
        writeUTF(value.company)
        writeUTF(value.title)
        writeUTF(value.department)
        writeUTF(value.email)
        writeUTF(value.city)
        writeUTF(value.country)
        writeUTF(value.phonenumber)
        writeUTF(value.comment)
    }

    override fun deserialize(input: DataInput2, available: Int): Model = with(input) {
        val firstname = readUTF()
        val lastname = readUTF()
        val company = readUTF()
        val title = readUTF()
        val department = readUTF()
        val email = readUTF()
        val city = readUTF()
        val country = readUTF()
        val phonenumber = readUTF()
        val comment = readUTF()
        return Model(firstname, lastname, company, title, department, email, city, country, phonenumber, comment)
    }
}

fun Array<String?>.csvjoin(): String = this.map { "\"$it\"" }.joinToString(";")
