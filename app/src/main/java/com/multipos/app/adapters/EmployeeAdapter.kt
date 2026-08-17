package com.multipos.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.multipos.app.R
import com.multipos.app.data.entities.Usuario
import com.multipos.app.databinding.ItemEmployeeBinding

class EmployeeAdapter(
    private val currentUserId: Int,
    private val managerRole: String,
    private val onActiveChanged: (Usuario, Boolean) -> Unit
) :
    ListAdapter<Usuario, EmployeeAdapter.EmployeeViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder =
        EmployeeViewHolder(ItemEmployeeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) = holder.bind(getItem(position))

    inner class EmployeeViewHolder(private val binding: ItemEmployeeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: Usuario) = with(binding) {
            txtEmployeeName.text = user.nombre
            txtEmployeeDetail.text = itemView.context.getString(
                R.string.employee_detail_format,
                user.rol.lowercase().replaceFirstChar(Char::uppercase),
                user.usuario
            )
            switchEmployeeActive.setOnCheckedChangeListener(null)
            switchEmployeeActive.isChecked = user.activo
            switchEmployeeActive.isEnabled =
                user.rol != Usuario.ROL_PROPIETARIO &&
                    user.id != currentUserId &&
                    (managerRole == Usuario.ROL_PROPIETARIO || user.rol != Usuario.ROL_ADMINISTRADOR)
            switchEmployeeActive.setOnCheckedChangeListener { _, active -> onActiveChanged(user, active) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Usuario>() {
        override fun areItemsTheSame(oldItem: Usuario, newItem: Usuario): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Usuario, newItem: Usuario): Boolean = oldItem == newItem
    }
}
