package ma.ensa.graphqltp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import ma.ensa.graphqltp.adapter.ComptesAdapter
import ma.ensa.graphqltp.data.MainViewModel
import ma.ensa.graphqltp.type.TypeCompte

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var addButton: Button
    private lateinit var totalCountText: TextView
    private lateinit var totalSumText: TextView
    private lateinit var averageText: TextView
    private lateinit var statsCard: View
    private lateinit var typeGroup: RadioGroup

    private val viewModel: MainViewModel by viewModels()
    private val comptesAdapter = ComptesAdapter(
        onDeleteClick = { id ->
            showDeleteConfirmationDialog(id)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupRecyclerView()
        setupAddButton()
        setupTypeFilter()
        observeViewModel()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.comptesRecyclerView)
        addButton = findViewById(R.id.addCompteButton)
        totalCountText = findViewById(R.id.totalCountText)
        totalSumText = findViewById(R.id.totalSumText)
        averageText = findViewById(R.id.averageText)
        statsCard = findViewById(R.id.statsCard)
        typeGroup = findViewById(R.id.typeGroup)
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            adapter = comptesAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupAddButton() {
        addButton.setOnClickListener {
            showAddCompteDialog()
        }
    }

    private fun setupTypeFilter() {
        typeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.courantRadio -> viewModel.loadComptesByType(TypeCompte.COURANT)
                R.id.epargneRadio -> viewModel.loadComptesByType(TypeCompte.EPARGNE)
                R.id.allRadio -> viewModel.loadComptes()
            }
        }
    }

    private fun showDeleteConfirmationDialog(id: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete this account?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCompte(id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        // Observe the list of comptes
        viewModel.comptesState.observe(this) { state ->
            when (state) {
                is MainViewModel.UIState.Loading -> {
                    // Show loading UI for comptes
                }
                is MainViewModel.UIState.Success -> {
                    comptesAdapter.updateList(state.data)
                }
                is MainViewModel.UIState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observe filtered comptes
        viewModel.filteredComptesState.observe(this) { state ->
            when (state) {
                is MainViewModel.UIState.Loading -> {
                    // Show loading UI for filtered comptes
                }
                is MainViewModel.UIState.Success -> {
                    // Convert filtered comptes to the format expected by the adapter
                    val adaptedComptes = state.data.map { filtered ->
                        GetAllComptesQuery.AllCompte(
                            filtered.id,
                            filtered.solde,
                            filtered.dateCreation,
                            filtered.type
                        )
                    }
                    comptesAdapter.updateList(adaptedComptes)
                }
                is MainViewModel.UIState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Observe the total solde statistics
        viewModel.totalSoldeState.observe(this) { state ->
            when (state) {
                is MainViewModel.UIState.Loading -> {
                    statsCard.visibility = View.INVISIBLE
                }
                is MainViewModel.UIState.Success -> {
                    statsCard.visibility = View.VISIBLE
                    totalCountText.text = "Nombre total des comptes: ${state.data?.count}"
                    totalSumText.text = "Total des balances: ${state.data?.sum} dh"
                    averageText.text = "Moyenne des balances: ${state.data?.average} dh"
                }
                is MainViewModel.UIState.Error -> {
                    statsCard.visibility = View.INVISIBLE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showAddCompteDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_compte, null)
        val soldeInput = dialogView.findViewById<TextInputEditText>(R.id.soldeInput)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.typeGroup)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Account")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val solde = soldeInput.text.toString().toDoubleOrNull()
                val selectedId = typeGroup.checkedRadioButtonId
                val typeRadioButton = dialogView.findViewById<RadioButton>(selectedId)
                val type = when (typeRadioButton.text.toString().uppercase()) {
                    "COURANT" -> TypeCompte.COURANT
                    "EPARGNE" -> TypeCompte.EPARGNE
                    else -> TypeCompte.COURANT
                }

                if (solde != null) {
                    viewModel.saveCompte(solde, type)
                } else {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}