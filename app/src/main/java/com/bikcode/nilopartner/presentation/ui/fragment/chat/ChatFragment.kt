package com.bikcode.nilopartner.presentation.ui.fragment.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.Message
import com.bikcode.nilopartner.data.model.OrderDTO
import com.bikcode.nilopartner.databinding.FragmentChatBinding
import com.bikcode.nilopartner.presentation.adapter.ChatAdapter
import com.bikcode.nilopartner.presentation.listeners.OnChatListener
import com.bikcode.nilopartner.presentation.listeners.OrderAux
import com.bikcode.nilopartner.presentation.util.Constants.PATH_CHAT
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ChatFragment : Fragment(), OnChatListener {

    private var _binding: FragmentChatBinding? = null
    private val binding: FragmentChatBinding get() = _binding!!
    private var order: OrderDTO? = null
    private val chatAdapter: ChatAdapter by lazy { ChatAdapter(mutableListOf(), this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getOrder()
        setupRecyclerView()
        setupButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.title = getString(R.string.order_history)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun deleteMessage(message: Message) {
        order?.let {
            val database = Firebase.database
            val messageRef = database.getReference(PATH_CHAT).child(it.id).child(message.id)
            messageRef.removeValue { error, ref ->
                if (error != null) {
                    Snackbar.make(binding.root,
                        getString(R.string.error_delete_message),
                        Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root,
                        getString(R.string.delete_message),
                        Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButtons() {
        with(binding) {
            ibSend.setOnClickListener {
                sendMessage()
            }
        }
    }

    private fun sendMessage() {
        order?.let { order ->
            val database = Firebase.database
            val chatRef = database.getReference(PATH_CHAT).child(order.id)
            val user = FirebaseAuth.getInstance().currentUser

            user?.let {
                val message = Message(
                    message = binding.tieMessage.text.toString().trim(),
                    sender = if(order.sellerId != "") order.sellerId else user.uid
                )

                binding.tieMessage.isEnabled = false

                chatRef.push().setValue(message)
                    .addOnSuccessListener {
                        binding.tieMessage.setText("")
                    }.addOnCompleteListener {
                        binding.tieMessage.isEnabled = true
                    }
            }
        }
    }

    private fun getOrder() {
        order = (activity as? OrderAux)?.getOrderSelected()
        order?.let {
            setupActionBar()
            setupRealtimeDatabase()
        }
    }

    private fun setupActionBar() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.title = getString(R.string.chat_title)
        }
    }

    private fun setupRecyclerView() {
        binding.rvChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context).also {
                it.stackFromEnd = true
            }
        }
    }

    private fun setupRealtimeDatabase() {
        order?.let { order ->
            val database = Firebase.database
            val chatRef = database.getReference(PATH_CHAT).child(order.id)

            val childListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let {
                        chatAdapter.add(it)
                        binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let {
                        chatAdapter.update(it)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    getMessage(snapshot)?.let {
                        chatAdapter.delete(it)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(binding.root,
                        getString(R.string.error_chat),
                        Snackbar.LENGTH_SHORT).show()
                }
            }
            chatRef.addChildEventListener(childListener)
        }
    }

    private fun getMessage(snapshot: DataSnapshot): Message? {
        snapshot.getValue(Message::class.java)?.let { message ->
            snapshot.key?.let {
                message.id = it
            }
            FirebaseAuth.getInstance().currentUser?.let {
                message.uid = it.uid
            }
            return message
        }
        return null
    }
}