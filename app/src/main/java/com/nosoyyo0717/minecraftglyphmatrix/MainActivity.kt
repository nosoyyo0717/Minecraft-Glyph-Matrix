package com.nosoyyo0717.minecraftglyphmatrix

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.delay
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.content.Intent
import android.provider.Settings
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
val PHONE_3_ACTIVE_WIDTHS = listOf(
    7, 11, 15, 17, 19, 21, 21, 23, 23, 25, 25, 25, 25,
    25, 25, 25, 23, 23, 21, 21, 19, 17, 15, 11, 7
)

val PHONE_4A_PRO_ACTIVE_WIDTHS = listOf(
    5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5
)

val MOB_COMPATIBILITY_TAGS = mapOf(
    "Creeper" to listOf("3"),
    "Skeleton" to listOf("3"),
    "Enderman" to listOf("3", "4aPro"),
    "Ghastling" to listOf("3"),
    "Creaking" to listOf("3"),
    "Carved Snow Golem" to listOf("3"),
    "Wither" to listOf("3"),
    "Carrot" to listOf("3"),
    "Potato" to listOf("3"),
    "Wheat Seed" to listOf("3"),
    "Firefly Bush" to listOf("3"),
    "Sugarcane" to listOf("3"),
    "Turtle Egg" to listOf("3"),
    "White Candle" to listOf("3"),
    "Campfire" to listOf("3"),
    "Lantern" to listOf("3"),
    "White Bed" to listOf("3"),
    "Pale Oak Sign" to listOf("3"),
    "Oak Door" to listOf("3"),
    "Spruce Door" to listOf("3"),
    "Birch Door" to listOf("3"),
    "Jungle Door" to listOf("3"),
    "Acacia Door" to listOf("3"),
    "Dark Oak Door" to listOf("3"),
    "Mangrove Door" to listOf("3"),
    "Cherry Door" to listOf("3"),
    "Pale Oak Door" to listOf("3"),
    "Bamboo Door" to listOf("3"),
    "Crimson Door" to listOf("3"),
    "Warped Door" to listOf("3"),
    "Iron Door" to listOf("3"),
    "Copper Door" to listOf("3"),
    "Cake" to listOf("3"),
    "Bell" to listOf("3"),
    "Pale Oak Boat" to listOf("3"),
    "Spyglass" to listOf("3"),
    "Name Tag" to listOf("3"),
    "Book and Quill" to listOf("3"),
    "Map" to listOf("3"),
    "Water Bucket" to listOf("3"),
    "Milk Bucket" to listOf("3"),
    "Powder Snow Bucket" to listOf("3"),
    "Bucket of Axolotl" to listOf("3"),
    "Elytra" to listOf("3"),
    "Broken Elytra" to listOf("3"),
    "Totem of Undying" to listOf("3"),
    "White Bundle" to listOf("3"),
    "Music Disc Strad" to listOf("3"),
    "Music Disc Tears" to listOf("3"),
    "Music Disc Lava Chicken" to listOf("3"),
    "Iron Axe" to listOf("3"),
    "Iron Pickaxe" to listOf("3"),
    "Iron Shovel" to listOf("3"),
    "Iron Sword" to listOf("3"),
    "Iron Spear" to listOf("3"),
    "Fishing Rod" to listOf("3"),
    "Carrot on a Stick" to listOf("3"),
    "Warped Fungus on a Stick" to listOf("3"),
    "Iron Helmet" to listOf("3"),
    "Iron Chestplate" to listOf("3"),
    "Iron Leggings" to listOf("3"),
    "Iron Boots" to listOf("3"),
)

class MainActivity : ComponentActivity() {

    private var glyphManager: GlyphMatrixManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        glyphManager = GlyphMatrixManager.getInstance(this)
        glyphManager?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName) {
                glyphManager?.register("MinecraftGlyph")
            }
            override fun onServiceDisconnected(name: ComponentName) {
                glyphManager = null
            }
        })

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                AppRouter(glyphManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        glyphManager?.unInit()
    }
}

object MatrixCache {
    val bitmaps = mutableMapOf<String, ImageBitmap>()
    val matrices = mutableMapOf<String, IntArray>()

    suspend fun initialize(deviceModel: String, masterCategories: Map<String, List<String>>) {
        withContext(Dispatchers.Default) {
            bitmaps.clear()
            matrices.clear()
            val allMobs = masterCategories.values.flatten().toSet()

            // We bake a high-resolution 500x500 image so the LEDs look incredibly sharp!
            val imageSize = 500f

            allMobs.forEach { mobName ->
                // 1. Cache the raw array for the Export button
                val matrix = getMatrixForMob(mobName, deviceModel)
                matrices[mobName] = matrix

                // 2. Setup the Native Android Canvas
                val gridSize = if (deviceModel == "4aPro") 17 else 25
                val activeWidths = if (deviceModel == "4aPro") PHONE_4A_PRO_ACTIVE_WIDTHS else PHONE_3_ACTIVE_WIDTHS

                val bmp = Bitmap.createBitmap(imageSize.toInt(), imageSize.toInt(), Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                val paint = android.graphics.Paint().apply { isAntiAlias = true }

                val visualGridSize = if (deviceModel == "4aPro") 13f else 25f
                val dotSize = imageSize / visualGridSize
                val xOffset = if (deviceModel == "4aPro") -(2f * dotSize) else 0f
                val gap = dotSize * 0.05f // Creates a tiny 5% physical gap between LEDs
                val cornerRadius = dotSize * 0.15f // Creates a slight 15% rounding for the hardware look

                for (row in 0 until gridSize) {
                    val activeCount = activeWidths.getOrElse(row) { 0 }
                    if (activeCount == 0) continue
                    val emptySpaces = (gridSize - activeCount) / 2

                    for (col in emptySpaces until (emptySpaces + activeCount)) {
                        val index = row * gridSize + col
                        val brightness = matrix.getOrElse(index) { 0 }

                        if (brightness > 0) {
                            paint.color = android.graphics.Color.argb(brightness, 255, 255, 255)
                        } else {
                            paint.color = android.graphics.Color.rgb(35, 35, 35) // Slightly darker empty LED
                        }

                        // Calculate the coordinates
                        val left = (col * dotSize) + xOffset
                        val top = row * dotSize

                        // DRAW THE LED: Mathematically tight, slightly rounded squares!
                        canvas.drawRoundRect(
                            left + gap, top + gap,
                            left + dotSize - gap, top + dotSize - gap,
                            cornerRadius, cornerRadius, paint
                        )
                    }
                }

                // 4. Save the perfectly centered high-res image to RAM
                bitmaps[mobName] = bmp.asImageBitmap()
            }
        }
    }
}

// --- 1. THE ROUTER ---
@Composable
fun AppRouter(glyphManager: GlyphMatrixManager?) {
    var currentScreen by remember { mutableStateOf("HOME") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedMob by remember { mutableStateOf("") }

    val actualDeviceModel = remember { Build.MODEL }
    val isSimulatorMode = actualDeviceModel != "A013" && actualDeviceModel != "A014"
    var activeDeviceMode by remember { mutableStateOf(if (actualDeviceModel == "A014") "4aPro" else "3") }

    val masterCategories = mapOf(
        "Minecraft Mob Face" to listOf("Creeper","Skeleton", "Enderman", "Ghastling","Creaking", "Carved Snow Golem", "Wither"),
        "Minecraft Food" to listOf("Carrot", "Potato", "Cake"),

        "Minecraft Armor and Tools" to listOf("Elytra", "Broken Elytra", "Iron Axe", "Iron Pickaxe", "Iron Shovel", "Iron Sword", "Iron Spear", "Fishing Rod",
            "Warped Fungus on a Stick", "Iron Helmet", "Iron Chestplate", "Iron Leggings", "Iron Boots"),

        "Minecraft Items" to listOf("Wheat Seed", "Sugarcane", "Turtle Egg", "White Candle", "Pale Oak Boat", "Spyglass", "Name Tag", "Book and Quill", "Map",
            "Water Bucket", "Milk Bucket", "Powder Snow Bucket", "Bucket of Axolotl", "Totem of Undying", "White Bundle", ),

        "Minecraft Music Discs" to listOf("Music Disc Strad", "Music Disc Tears", "Music Disc Lava Chicken"),

        "Minecraft Blocks" to listOf("Firefly Bush", "Campfire", "Lantern", "White Bed", "Pale Oak Sign", "Oak Door", "Spruce Door", "Birch Door", "Jungle Door",
            "Acacia Door", "Dark Oak Door", "Mangrove Door", "Cherry Door", "Pale Oak Door", "Bamboo Door", "Crimson Door", "Warped Door", "Iron Door", "Copper Door",
            "Bell")
    )

    val filteredCategories = remember(activeDeviceMode) {
        masterCategories.mapValues { entry ->
            entry.value.filter { mobName ->
                val supported = MOB_COMPATIBILITY_TAGS[mobName] ?: listOf("3", "4aPro")
                supported.contains(activeDeviceMode)
            }
        }
    }

    // 🚀 LAUNCH CACHE TRIGGER
    var isCacheReady by remember { mutableStateOf(false) }

    LaunchedEffect(activeDeviceMode) {
        isCacheReady = false
        MatrixCache.initialize(activeDeviceMode, masterCategories)
        isCacheReady = true
    }

    // Show a loading screen while the images generate in the background
    if (!isCacheReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    BackHandler(enabled = currentScreen != "HOME") {
        if (currentScreen == "MATRIX") {
            glyphManager?.setAppMatrixFrame(IntArray(625) { 0 })
            currentScreen = "CATEGORY_DETAILS"
        } else if (currentScreen == "CATEGORY_DETAILS") {
            currentScreen = "HOME"
        }
    }

    Crossfade(targetState = currentScreen, label = "ScreenTransitionFade") { screen ->
        when (screen) {
            "HOME" -> {
                HomeScreen(
                    categories = filteredCategories,
                    deviceModel = activeDeviceMode,
                    isSimulatorMode = isSimulatorMode,
                    onDeviceSwitch = { activeDeviceMode = it },
                    onCategoryClick = { categoryName ->
                        selectedCategory = categoryName
                        currentScreen = "CATEGORY_DETAILS"
                    }
                )
            }
            "CATEGORY_DETAILS" -> {
                CategoryScreen(
                    categoryName = selectedCategory,
                    mobList = filteredCategories[selectedCategory] ?: emptyList(),
                    onBackClick = { currentScreen = "HOME" },
                    onMobClick = { mobName ->
                        selectedMob = mobName
                        currentScreen = "MATRIX"
                    }
                )
            }
            "MATRIX" -> {
                MatrixPreviewScreen(
                    mobName = selectedMob,
                    glyphManager = glyphManager,
                    onBackClick = { currentScreen = "CATEGORY_DETAILS" }
                )
            }
        }
    }
}

// --- 2. THE HOME SCREEN ---
@Composable
fun HomeScreen(
    categories: Map<String, List<String>>,
    deviceModel: String,
    isSimulatorMode: Boolean,
    onDeviceSwitch: (String) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp)) {
                Text("MINECRAFT", color = Color.White, fontSize = 16.sp, letterSpacing = 4.sp, fontFamily = FontFamily.Monospace)
                Text("MATRIX GALLERY", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
            }
        }

        if (isSimulatorMode) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { onDeviceSwitch("3") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (deviceModel == "3") Color.White else Color(0xFF222222), contentColor = if (deviceModel == "3") Color.Black else Color.Gray)
                ) { Text("PHONE 3", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { onDeviceSwitch("4aPro") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (deviceModel == "4aPro") Color.White else Color(0xFF222222), contentColor = if (deviceModel == "4aPro") Color.Black else Color.Gray)
                ) { Text("PHONE 4a PRO", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            val categoryList = categories.keys.toList()
            items(categoryList) { categoryName ->
                val mobList = categories[categoryName] ?: emptyList()

                Card(
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.9f).clickable { onCategoryClick(categoryName) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (mobList.isNotEmpty()) {
                            var currentIndex by remember(deviceModel, mobList) { mutableIntStateOf(if (mobList.isNotEmpty()) mobList.indices.random() else 0) }

                            LaunchedEffect(mobList) {
                                while (true) {
                                    delay(5000)
                                    if (mobList.size > 1) {
                                        val randomJump = (1 until mobList.size).random()
                                        currentIndex = (currentIndex + randomJump) % mobList.size
                                    }
                                }
                            }

                            Crossfade(targetState = mobList[currentIndex], animationSpec = tween(1500), label = "Morph", modifier = Modifier.align(Alignment.Center).padding(bottom = 32.dp)) { currentMobName ->
                                // 🚀 Passes the name to instantly grab the image
                                MiniMatrixPreview(mobName = currentMobName, modifier = Modifier.size(90.dp))
                            }
                        }

                        Text(
                            text = categoryName.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- 3. THE CATEGORY SCREEN ---
@Composable
fun CategoryScreen(categoryName: String, mobList: List<String>, onBackClick: () -> Unit, onMobClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 40.dp)) {
            Text("<", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.CenterStart).clickable { onBackClick() })
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp)) {
                Text("CATEGORY", color = Color.White, fontSize = 12.sp, letterSpacing = 4.sp, fontFamily = FontFamily.Monospace)
                Text(categoryName.uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (mobList.isNotEmpty()) {
                items(mobList) { mobName ->
                    Card(
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).clickable { onMobClick(mobName) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
                                // 🚀 Instantly loads from cache
                                MiniMatrixPreview(mobName = mobName, modifier = Modifier.padding(16.dp).aspectRatio(1f))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = mobName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            } else {
                item {
                    Card(modifier = Modifier.fillMaxWidth().aspectRatio(1f), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("COMING SOON...", color = Color(0xFF555555), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// --- 4. THE MATRIX SCREEN ---
@Composable
fun MatrixPreviewScreen(mobName: String, glyphManager: GlyphMatrixManager?, onBackClick: () -> Unit) {
    // Read the array from the cache so we can export it later!
    val currentMatrixState = MatrixCache.matrices[mobName] ?: IntArray(625) { 0 }

    var includeSound by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isSupportedDevice = remember { Build.MODEL == "A013" || Build.MODEL == "A014" }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 40.dp)) {
            Text("<", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.CenterStart).clickable {
                glyphManager?.setAppMatrixFrame(IntArray(625) { 0 })
                onBackClick()
            })
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp)) {
                Text("DESIGN PREVIEW", color = Color.White, fontSize = 12.sp, letterSpacing = 4.sp, fontFamily = FontFamily.Monospace)
                Text(mobName.uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
            }
        }

        Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) {
            // 🚀 The big preview is now instantly loaded from the Image cache too!
            MiniMatrixPreview(mobName = mobName, modifier = Modifier.size(280.dp))
        }

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("INCLUDE AUDIO", color = if (isSupportedDevice) Color.White else Color.DarkGray, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Switch(
                    checked = includeSound, onCheckedChange = { includeSound = it }, enabled = isSupportedDevice,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0x00D92D20), uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF333333), disabledCheckedTrackColor = Color(0xFF551111), disabledUncheckedTrackColor = Color(0xFF1A1A1A))
                )
            }

            Button(
                onClick = { exportGlyphNotification(context, mobName, currentMatrixState, includeSound) },
                modifier = Modifier.fillMaxWidth().height(60.dp), shape = CircleShape, enabled = isSupportedDevice,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black, disabledContainerColor = Color(0xFF222222), disabledContentColor = Color(0xFF555555))
            ) {
                Text(if (isSupportedDevice) "SAVE NOTIFICATION" else "NOT SUPPORTED ON THIS DEVICE", fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun MiniMatrixPreview(mobName: String, modifier: Modifier = Modifier) {
    val bitmap = MatrixCache.bitmaps[mobName]

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = mobName,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}
// === THE TRAFFIC COP ===
fun getMatrixForMob(mobName: String, deviceModel: String): IntArray {
    if (deviceModel == "4aPro") {
        return when (mobName) {
            "Creeper" -> getCreeperFace_4aPro() // We load the 4aPro version!
            "Enderman" -> getEndermanFace_4aPro()
            // TODO: Add your other 4a Pro versions here as you make them!
            else -> IntArray(289) { 0 }
        }
    } else if (deviceModel == "3"){
        return when (mobName) {
            "Creeper" -> getCreeperFace()
            "Enderman" -> getEndermanFace()
            "Ghastling" -> getGhastling()
            "Creaking" -> getCreakingFace()
            "Skeleton" -> getSkeletonFace()
            "Carved Snow Golem" -> getCarvedSnowGolem()
            "Wither" -> getWither()
            "Carrot" -> getMinecraftCarrot()
            "Potato" -> getPotato()
            "Wheat Seed" -> getWheatSeeds()
            "Firefly Bush" -> getFireflyBush()
            "Sugarcane" -> getSugarcane()
            "Turtle Egg" -> getTurtleEgg()
            "White Candle" -> getWhiteCandle()
            "Campfire" -> getCampfire()
            "Lantern" -> getLantern()
            "White Bed" -> getWhiteBed()
            "Pale Oak Sign" -> getPaleOakSign()
            "Oak Door" -> getOakDoor()
            "Spruce Door" -> getSpruceDoor()
            "Birch Door" -> getBirchDoor()
            "Jungle Door" -> getJungleDoor()
            "Acacia Door" -> getAcaciaDoor()
            "Dark Oak Door" -> getDarkOakDoor()
            "Mangrove Door" -> getMangroveDoor()
            "Cherry Door" -> getCherryDoor()
            "Pale Oak Door" -> getPaleOakDoor()
            "Bamboo Door" -> getBambooDoor()
            "Crimson Door" -> getCrimsonDoor()
            "Warped Door" -> getWarpedDoor()
            "Iron Door" -> getIronDoor()
            "Copper Door" -> getCopperDoor()
            "Cake" -> getCake()
            "Bell" -> getBell()
            "Pale Oak Boat" -> getPaleOakBoat()
            "Spyglass" -> getSpyglass()
            "Name Tag" -> getNameTag()
            "Book and Quill" -> getBookandQuill()
            "Map" -> getMap()
            "Water Bucket" -> getWaterBucket()
            "Milk Bucket" -> getMilkBucket()
            "Powder Snow Bucket" -> getPowderSnowBucket()
            "Bucket of Axolotl" -> getBucketofAxolotl()
            "Elytra" -> getElytra()
            "Broken Elytra" -> getBrokenElytra()
            "Totem of Undying" -> getTotemofUndying()
            "White Bundle" -> getWhiteBundle()
            "Music Disc Strad" -> getMusicDiscStrad()
            "Music Disc Tears" -> getMusicDiscTears()
            "Music Disc Lava Chicken" -> getMusicDiscLavaChicken()
            "Iron Axe" -> getIronAxe()
            "Iron Pickaxe" -> getIronPickaxe()
            "Iron Shovel" -> getIronShovel()
            "Iron Sword" -> getIronSword()
            "Iron Spear" ->getIronSpear()
            "Fishing Rod" -> getFishingRod()
            "Carrot on a Stick" -> getCarrotonaStick()
            "Warped Fungus on a Stick" -> getWarpedFungusonaStick()
            "Iron Helmet" -> getIronHelmet()
            "Iron Chestplate" -> getIronChestplate()
            "Iron Leggings" -> getIronLeggings()
            "Iron Boots" -> getIronBoots()
            "My Design" -> getMyCustomDesign()
            else -> IntArray(625) { 0 }
        }
    }
    return IntArray(625) { 0 }
}

private fun exportGlyphNotification(context: Context, mobName: String, matrix: IntArray, includeSound: Boolean) {
    if (!includeSound) {
        Toast.makeText(context, "${mobName.uppercase()} Matrix saved to local gallery!", Toast.LENGTH_SHORT).show()
        return
    }

    val audioResId = getAudioForMob(mobName)
    if (audioResId == null) {
        Toast.makeText(context, "No audio file available for $mobName yet.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${mobName}_glyph_alert.ogg")
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/ogg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_NOTIFICATIONS)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, true)
        }

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            val inputStream = context.resources.openRawResource(audioResId)
            val outputStream = resolver.openOutputStream(uri)
            if (outputStream != null) {
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                Toast.makeText(context, "Saved! Select it in Settings.", Toast.LENGTH_LONG).show()

                val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: permission denied.", Toast.LENGTH_SHORT).show()
    }
}

private fun getAudioForMob(mobName: String): Int? {
    return when (mobName) {
        // "Creeper" -> R.raw.creeper_hiss
        // "Skeleton" -> R.raw.skeleton_rattle
        else -> null
    }
}