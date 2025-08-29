package com.example.workmatepokemon.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.workmatepokemon.R
import com.example.workmatepokemon.data.Pokemon

class PokemonAdapter (
    private val pokemonList: ArrayList<Pokemon>,
    private val context: Context
) : RecyclerView.Adapter<PokemonAdapter.PokemonViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PokemonAdapter.PokemonViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.pokemon_card,
            parent, false
        )
        return PokemonViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PokemonAdapter.PokemonViewHolder, position: Int) {
        val pokemon = pokemonList.get(position)
        holder.pokemonName.text = pokemon.name

        val imageUrl = pokemon.sprites
        if (imageUrl != null) {
            Glide.with(holder.itemView.context)
                .asBitmap()
                .load(pokemon.sprites) // твой url
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap?>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.let { bmp ->
                            Palette.from(bmp).generate { palette ->
                                val bgColor = palette?.getDominantColor(
                                    ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
                                )
                                (holder.itemView as CardView).setCardBackgroundColor(bgColor ?: Color.GRAY)
                                // Цвета текста с контрастом
                                val textColor = palette?.getDominantSwatch()?.titleTextColor ?: Color.WHITE
                                holder.pokemonName.setTextColor(textColor)
                            }
                        }
                        return false
                    }

                })
                .placeholder(R.drawable.pokemon_logo)
                .error(R.drawable.error_image)
                .into(holder.pokemonImage)
        }

    }

    override fun getItemCount(): Int {
        return pokemonList.size
    }

    fun addPokemons(newPokemons: List<Pokemon>) {
        val startPos = pokemonList.size
        pokemonList.addAll(newPokemons)
        notifyItemRangeInserted(startPos, newPokemons.size)
    }

    fun clearPokemons() {
        pokemonList.clear()
        notifyDataSetChanged()
    }

    class PokemonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pokemonName: TextView = itemView.findViewById(R.id.pokemon_name)
        val pokemonImage: ImageView = itemView.findViewById(R.id.pokemon_image)
    }
}