package com.armonia.app.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armonia.app.domain.model.Instrument
import com.armonia.app.domain.model.Level
import com.armonia.app.domain.model.Profile
import com.armonia.app.ui.components.ArmoniaTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: Profile,
    onBack: () -> Unit,
    onSave: (Profile) -> Unit
) {
    var instrument by rememberSaveable(profile.instrument) { mutableStateOf(profile.instrument) }
    var level by rememberSaveable(profile.level) { mutableStateOf(profile.level) }
    val hasChanges = instrument != profile.instrument || level != profile.level

    Column(modifier = Modifier.fillMaxSize()) {
        ArmoniaTopBar(title = "Profilo e privacy", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .size(68.dp)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = instrument.symbol,
                            fontSize = 28.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.size(14.dp))
                Column {
                    Text(
                        text = "Il tuo percorso",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "${instrument.label} · ${level.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Strumento",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Instrument.entries.forEach { item ->
                    FilterChip(
                        selected = instrument == item,
                        onClick = { instrument = item },
                        label = { Text("${item.symbol} ${item.label}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Livello",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Level.entries.forEach { item ->
                    FilterChip(
                        selected = level == item,
                        onClick = { level = item },
                        label = { Text(item.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "La modifica aggiorna subito esercizi e contenuti consigliati. I progressi restano invariati.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSave(Profile(instrument, level)) },
                enabled = hasChanges,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Salva le preferenze")
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Privacy by design",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(10.dp))

            PrivacyCard(
                icon = Icons.Outlined.Lock,
                title = "Microfono solo quando serve",
                body = "Il permesso microfono viene chiesto solo per accordatore e pratica guidata. " +
                    "L'audio è elaborato in memoria, frame per frame, e non viene mai registrato o caricato. " +
                    "Quando cambi schermata o metti l'app in background, l'ascolto si ferma."
            )

            Spacer(modifier = Modifier.height(10.dp))

            PrivacyCard(
                icon = Icons.Outlined.CloudOff,
                title = "Niente rete, tutto offline",
                body = "L'app non dichiara il permesso Internet. Spartiti, lezioni, esercizi e progressi " +
                    "sono disponibili senza connessione. I dati del profilo e delle sessioni restano in locale " +
                    "e il backup Android è disattivato."
            )

            Spacer(modifier = Modifier.height(10.dp))

            PrivacyCard(
                icon = Icons.Outlined.Person,
                title = "Nessun account obbligatorio",
                body = "Non chiediamo email, nome o password. L'onboarding serve solo a personalizzare il percorso " +
                    "e resta sul dispositivo."
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Contenuti di pubblico dominio e studi originali",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Armonia AI · MVP 0.1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrivacyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
