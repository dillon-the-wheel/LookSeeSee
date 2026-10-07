package com.lookseesee.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lookseesee.app.R
import com.lookseesee.app.data.MediaPickKey
import com.lookseesee.app.data.model.Album
import com.lookseesee.app.data.model.MediaEntry

/**
 * Full-screen browser for one album (or the cross-album "My Picks" set): a grid of
 * every photo/video in it with a checkmark to hand-pick specific items, separate
 * from the all-or-nothing album toggle on the main Setup screen.
 */
@Composable
fun AlbumDetailScreen(
    album: Album?,
    isPicksView: Boolean,
    isWholeAlbumSelected: Boolean,
    media: List<MediaEntry>,
    isLoading: Boolean,
    selectedKeys: Set<String>,
    onToggleWholeAlbum: () -> Unit,
    onToggleItem: (MediaEntry) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Text(
                text = album?.displayName ?: stringResource(R.string.setup_my_picks),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        if (!isPicksView && album != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.padding(end = 12.dp)) {
                    Text(stringResource(R.string.setup_album_detail_whole_album))
                    Text(
                        text = stringResource(R.string.setup_album_detail_whole_album_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = isWholeAlbumSelected, onCheckedChange = { onToggleWholeAlbum() })
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (media.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.setup_album_detail_empty))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(media.size) { index ->
                    val entry = media[index]
                    val picked = isWholeAlbumSelected || MediaPickKey.encode(entry.id, entry is MediaEntry.Video) in selectedKeys
                    val locked = isWholeAlbumSelected && !isPicksView
                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .aspectRatio(1f)
                            .clickable(enabled = !locked) { onToggleItem(entry) },
                    ) {
                        AsyncImage(
                            model = entry.uri,
                            contentDescription = stringResource(R.string.cd_grid_thumbnail),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (entry is MediaEntry.Video) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        Icon(
                            imageVector = if (picked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (picked) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.White.copy(alpha = if (picked) 1f else 0.4f), CircleShape),
                        )
                    }
                }
            }
        }
    }
}
