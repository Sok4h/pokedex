package com.sokah.pokedex

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.sokah.pokedex.databinding.ActivityMainBinding
import com.sokah.pokedex.model.Pokemon
import com.sokah.pokedex.network.PokeService
import com.sokah.pokedex.view.PokemonAdapter
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private var coroutineJob: Job? = null
    private val api = PokeService()

    private var pokemon: Pokemon? = null
    private lateinit var pokemonAdapter: PokemonAdapter

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var username: String

    private lateinit var listener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        username = intent.getStringExtra("username")!!
        pokemonAdapter = PokemonAdapter(username)

        binding.inputPokemon.setEndIconOnClickListener {
            searchPokemon()
        }

        binding.btnSearchHome.setOnClickListener {
            searchPokemon()
        }

        binding.rvMyPokemonsHome.apply {
            layoutManager = GridLayoutManager(applicationContext, 2)
            adapter = pokemonAdapter
            //layoutManager = LinearLayoutManager(applicationContext)
        }

        listener = listenPokemons()

        binding.inputQuery.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().isEmpty()) {
                    listener = listenPokemons()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        binding.inputQuery.setEndIconOnClickListener {

            val filterValue = binding.inputQuery.editText?.text.toString()
            if (filterValue.isEmpty()) {
                listener = listenPokemons()
            } else {
                listener = filterPokemons(filterValue)
            }
        }

    }

    private fun filterPokemons(filterValue: String): ListenerRegistration {
        return Firebase.firestore
            .collection("users")
            .document(username)
            .collection("pokemons")
            .orderBy("date")
            .whereEqualTo("name", filterValue)
            .addSnapshotListener { snapshot, e ->
                pokemonAdapter.clear()
                if (e != null) {
                    Toast.makeText(
                        this,
                        e.message,
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(">>>", e.message.toString())
                    return@addSnapshotListener
                }

                if (snapshot !== null && !snapshot.isEmpty) {
                    snapshot.forEach {
                        val newPokemon = it.toObject(Pokemon::class.java)
                        newPokemon.uid = it.id

                        pokemonAdapter.addPokemon(newPokemon)
                    }

                } else {
                    Toast.makeText(
                        this,
                        "No se encontró el pokemon!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun listenPokemons(): ListenerRegistration {

       return Firebase.firestore
            .collection("users")
            .document(username)
            .collection("pokemons")
            .orderBy("date")
            .addSnapshotListener { snapshot, e ->
                pokemonAdapter.clear()
                if (e != null) {
                    Toast.makeText(
                        this,
                        "No se pudo cargar la información de tus pokemons",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot !== null && !snapshot.isEmpty) {
                    snapshot.forEach {
                        val newPokemon = it.toObject(Pokemon::class.java)
                        newPokemon.uid = it.id

                        pokemonAdapter.addPokemon(newPokemon)
                    }

                } else {
                    Toast.makeText(
                        this,
                        "Aun no tienes pokemons, atrapa tu primer pokemon!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

    }

    private fun searchPokemon() {
        if (binding.inputPokemon.editText?.text!!.isNotEmpty()) {
            coroutineJob = CoroutineScope(Dispatchers.IO).launch {

                pokemon = api.findPokemon(binding.inputPokemon.editText?.text.toString())

                if (pokemon == null) {

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            "El pokemon buscado no existe",
                            Toast.LENGTH_SHORT
                        ).show()

                    }

                } else {

                    val intent = Intent(applicationContext, PokemonActivity::class.java)
                    val gson = Gson()

                    intent.putExtra("pokemon", gson.toJson(pokemon))
                    intent.putExtra("username", username)
                    startActivity(intent)
                }

            }
        } else {

            Toast.makeText(this, "Ponga un nombre", Toast.LENGTH_SHORT).show()
        }
    }
}