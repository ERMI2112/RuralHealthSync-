package com.example.ruralhealthsync.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ruralhealthsync.data.remote.UserDto
import com.example.ruralhealthsync.databinding.UserItemBinding

class UserAdapter(
    private val onDeleteClick: (UserDto) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users: List<UserDto> = emptyList()

    fun setUsers(newList: List<UserDto>) {
        users = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(private val binding: UserItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: UserDto) {
            binding.userNameText.text = "${user.full_name} (@${user.username})"
            binding.userRoleText.text = "Role: ${user.role}"

            binding.deleteUserButton.setOnClickListener {
                onDeleteClick(user)
            }
        }
    }
}
