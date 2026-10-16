package com.gentlemanjoel.jot.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps a stored icon key to its vector. Unknown keys fall back to a plain note. */
fun iconFor(key: String): ImageVector = when (key) {
    // ---- general ----
    "check" -> Icons.Filled.CheckCircle
    "star" -> Icons.Filled.Star
    "flag" -> Icons.Filled.Flag
    "bolt" -> Icons.Filled.Bolt
    "heart" -> Icons.Filled.Favorite
    "pin" -> Icons.Filled.PushPin
    "bookmark" -> Icons.Filled.Bookmark
    "tag" -> Icons.Filled.Label
    "fire" -> Icons.Filled.LocalFireDepartment
    "clock" -> Icons.Filled.Schedule
    "alarm" -> Icons.Filled.Alarm

    // ---- productivity ----
    "task" -> Icons.Filled.TaskAlt
    "list" -> Icons.Filled.Checklist
    "idea" -> Icons.Filled.Lightbulb
    "target" -> Icons.Filled.TrackChanges
    "trending" -> Icons.Filled.TrendingUp
    "timer" -> Icons.Filled.Timer
    "calendar" -> Icons.Filled.CalendarMonth
    "event" -> Icons.Filled.Event
    "inbox" -> Icons.Filled.Inbox
    "send" -> Icons.Filled.Send
    "priority" -> Icons.Filled.PriorityHigh
    "edit" -> Icons.Filled.Edit

    // ---- office ----
    "work" -> Icons.Filled.Work
    "folder" -> Icons.Filled.Folder
    "archive" -> Icons.Filled.Archive
    "print" -> Icons.Filled.Print
    "mail" -> Icons.Filled.Mail
    "phone" -> Icons.Filled.Phone
    "badge" -> Icons.Filled.Badge
    "chart" -> Icons.Filled.BarChart
    "table" -> Icons.Filled.TableChart
    "receipt" -> Icons.Filled.Receipt
    "assignment" -> Icons.Filled.Assignment
    "briefcase" -> Icons.Filled.BusinessCenter

    // ---- money ----
    "money" -> Icons.Filled.AttachMoney
    "wallet" -> Icons.Filled.AccountBalanceWallet
    "card" -> Icons.Filled.CreditCard
    "bank" -> Icons.Filled.AccountBalance
    "savings" -> Icons.Filled.Savings
    "cart" -> Icons.Filled.ShoppingCart
    "shop" -> Icons.Filled.Storefront
    "gift" -> Icons.Filled.CardGiftcard
    "percent" -> Icons.Filled.Percent

    // ---- home ----
    "home" -> Icons.Filled.Home
    "bed" -> Icons.Filled.Bed
    "chair" -> Icons.Filled.Chair
    "cleaning" -> Icons.Filled.CleaningServices
    "laundry" -> Icons.Filled.LocalLaundryService
    "tools" -> Icons.Filled.Handyman
    "build" -> Icons.Filled.Build
    "yard" -> Icons.Filled.Yard
    "pets" -> Icons.Filled.Pets
    "plant" -> Icons.Filled.LocalFlorist
    "key" -> Icons.Filled.VpnKey

    // ---- cooking ----
    "restaurant" -> Icons.Filled.Restaurant
    "cafe" -> Icons.Filled.LocalCafe
    "pizza" -> Icons.Filled.LocalPizza
    "cake" -> Icons.Filled.Cake
    "icecream" -> Icons.Filled.Icecream
    "wine" -> Icons.Filled.WineBar
    "beer" -> Icons.Filled.SportsBar
    "egg" -> Icons.Filled.Egg
    "bakery" -> Icons.Filled.BakeryDining
    "grocery" -> Icons.Filled.LocalGroceryStore
    "kitchen" -> Icons.Filled.Kitchen
    "recipe" -> Icons.Filled.MenuBook

    // ---- sports ----
    "run" -> Icons.Filled.DirectionsRun
    "bike" -> Icons.Filled.DirectionsBike
    "gym" -> Icons.Filled.FitnessCenter
    "soccer" -> Icons.Filled.SportsSoccer
    "basketball" -> Icons.Filled.SportsBasketball
    "football" -> Icons.Filled.SportsFootball
    "baseball" -> Icons.Filled.SportsBaseball
    "tennis" -> Icons.Filled.SportsTennis
    "golf" -> Icons.Filled.SportsGolf
    "hike" -> Icons.Filled.Hiking
    "swim" -> Icons.Filled.Pool
    "ski" -> Icons.Filled.DownhillSkiing

    // ---- entertainment ----
    "movie" -> Icons.Filled.Movie
    "tv" -> Icons.Filled.Tv
    "music" -> Icons.Filled.MusicNote
    "headphones" -> Icons.Filled.Headphones
    "game" -> Icons.Filled.SportsEsports
    "camera" -> Icons.Filled.PhotoCamera
    "photo" -> Icons.Filled.Image
    "mic" -> Icons.Filled.Mic
    "book" -> Icons.Filled.Book
    "palette" -> Icons.Filled.Palette
    "theater" -> Icons.Filled.TheaterComedy
    "radio" -> Icons.Filled.Radio

    // ---- travel ----
    "car" -> Icons.Filled.DirectionsCar
    "flight" -> Icons.Filled.Flight
    "train" -> Icons.Filled.Train
    "map" -> Icons.Filled.Map
    "place" -> Icons.Filled.Place
    "beach" -> Icons.Filled.BeachAccess
    "luggage" -> Icons.Filled.Luggage
    "hotel" -> Icons.Filled.Hotel
    "fuel" -> Icons.Filled.LocalGasStation
    "explore" -> Icons.Filled.Explore

    // ---- health ----
    "health" -> Icons.Filled.MonitorHeart
    "spa" -> Icons.Filled.Spa
    "water" -> Icons.Filled.WaterDrop
    "sleep" -> Icons.Filled.Bedtime
    "medical" -> Icons.Filled.MedicalServices
    "pill" -> Icons.Filled.Medication

    // ---- tech ----
    "code" -> Icons.Filled.Code
    "laptop" -> Icons.Filled.Laptop
    "mobile" -> Icons.Filled.PhoneAndroid
    "cloud" -> Icons.Filled.Cloud
    "wifi" -> Icons.Filled.Wifi
    "bug" -> Icons.Filled.BugReport
    "terminal" -> Icons.Filled.Terminal
    "database" -> Icons.Filled.Storage

    else -> Icons.Filled.Description
}

/**
 * Maps a stored color key to its color. Every value is tuned to sit on the
 * Sagan Slate background without vibrating against it. Unknown keys fall back
 * to the accent blue.
 */
fun colorFor(key: String): Color = when (key) {
    "sky" -> Color(0xFF63B4F0)
    "indigo" -> Color(0xFF6D7FE0)
    "violet" -> Color(0xFF9B7ED9)
    "orchid" -> Color(0xFFC07FD4)
    "pink" -> Color(0xFFDB7FA8)
    "rose" -> Color(0xFFE0758A)
    "red" -> Color(0xFFD9674A)
    "orange" -> Color(0xFFE0873D)
    "sun" -> Slate.Sun
    "amber" -> Color(0xFFD9A14A)
    "gold" -> Color(0xFFD4BC5A)
    "lime" -> Color(0xFFA8C25C)
    "green" -> Color(0xFF4CAF84)
    "emerald" -> Color(0xFF3FBF95)
    "teal" -> Color(0xFF45B8B0)
    "cyan" -> Color(0xFF4AC0D9)
    "steel" -> Color(0xFF6E93B8)
    "sand" -> Color(0xFFC2A585)
    "slate" -> Slate.Muted
    else -> Slate.Accent
}
