package com.androidvip.hebf.helpers

import androidx.collection.ArrayMap

object CPUDatabases {

    val data: Map<String, String>
        get() {
            return snapdragon + helio + exynos
        }

    private val snapdragon: ArrayMap<String, String>
        get() {
            val map = ArrayMap<String, String>()

            map["sm8250"] = "Qualcomm® Snapdragon™ 865"
            map["sm8150-ac"] = "Qualcomm® Snapdragon™ 855+"
            map["sm8150"] = "Qualcomm® Snapdragon™ 855"
            map["sdm850"] = "Qualcomm® Snapdragon™ 850"
            map["sdm845"] = "Qualcomm® Snapdragon™ 845"
            map["msm8998"] = "Qualcomm® Snapdragon™ 835"
            map["msm8996pro"] = "Qualcomm® Snapdragon™ 821"
            map["msm8996"] = "Qualcomm® Snapdragon™ 820"
            map["msm8994"] = "Qualcomm® Snapdragon™ 810"
            map["msm8992"] = "Qualcomm® Snapdragon™ 808"
            map["msm8974aa"] = "Qualcomm® Snapdragon™ 801"
            map["msm8974ab"] = "Qualcomm® Snapdragon™ 801"
            map["msm8974ac"] = "Qualcomm® Snapdragon™ 801"
            map["msm8974pro-ac"]  = "Qualcomm® Snapdragon™ 801"
            map["msm8974"] = "Qualcomm® Snapdragon™ 800"

            map["sm7250-aa"] = "Qualcomm® Snapdragon™ 765"
            map["SM7250-AB"] = "Qualcomm® Snapdragon™ 765G"
            map["sm7150-aa"] = "Qualcomm® Snapdragon™ 730"
            map["sm7150-ab"] = "Qualcomm® Snapdragon™ 730G"
            map["sdm712"] = "Qualcomm® Snapdragon™ 712"
            map["sdm710"] = "Qualcomm® Snapdragon™ 710"

            map["sm6150"] = "Qualcomm® Snapdragon™ 675"
            map["sdm670"] = "Qualcomm® Snapdragon™ 670"
            map["sm6125"] = "Qualcomm® Snapdragon™ 665"
            map["sdm660"] = "Qualcomm® Snapdragon™ 660"
            map["msm8976pro"] = "Qualcomm® Snapdragon™ 653"
            map["msm8976"] = "Qualcomm® Snapdragon™ 652"
            map["msm8956"] = "Qualcomm® Snapdragon™ 650"
            map["sdm636"] = "Qualcomm® Snapdragon™ 636"
            map["sdm632"] = "Qualcomm® Snapdragon™ 632"
            map["sdm630"] = "Qualcomm® Snapdragon™ 630"
            map["msm8953pro"] = "Qualcomm® Snapdragon™ 626"
            map["msm8953"] = "Qualcomm® Snapdragon™ 625"
            map["msm8952"] = "Qualcomm® Snapdragon™ 617"
            map["MSM8939v2"] = "Qualcomm® Snapdragon™ 616"
            map["msm8939"] = "Qualcomm® Snapdragon™ 615"
            map["msm8936"] = "Qualcomm® Snapdragon™ 610"

            map["sdm450"] = "Qualcomm® Snapdragon™ 450"
            map["sdm439"] = "Qualcomm® Snapdragon™ 439"
            map["msm8940"] = "Qualcomm® Snapdragon™ 435"
            map["msm8937"] = "Qualcomm® Snapdragon™ 430"
            map["sdm429"] = "Qualcomm® Snapdragon™ 429"
            map["msm8920"] = "Qualcomm® Snapdragon™ 427"
            map["msm8917"] = "Qualcomm® Snapdragon™ 425"
            map["msm8929"] = "Qualcomm® Snapdragon™ 415"
            map["msm8916"] = "Qualcomm® Snapdragon™ 410"
            map["msm8226"] = "Qualcomm® Snapdragon™ 400"

            map["msm8909aa"] = "Qualcomm® Snapdragon™ 212"
            map["msm8905"] = "Qualcomm® Snapdragon™ 205"

            return map
        }

    private val helio: ArrayMap<String, String>
        get() {
            val map = ArrayMap<String, String>()

            map["mt6795"] = "MediaTek Helio X10"
            map["mt6762m"] = "MediaTek Helio A22"

            map["mt6757dt"] = "MediaTek Helio P25"
            map["mt6757"] = "MediaTek Helio P20"
            map["mt6762"] = "MediaTek Helio P22"
            map["mt6755"] = "MediaTek Helio P10"

            map["mt6799"] = "MediaTek Helio X30"
            map["mt6797x"] = "MediaTek Helio X27"
            map["mt6797t"] = "MediaTek Helio X25"
            map["mt6797"] = "MediaTek Helio X20"

            return map
        }

    private val exynos: ArrayMap<String, String>
        get() {
            val map = ArrayMap<String, String>()

            map["samsungexynos9810"] = "Samsung Exynos 9 9810"
            map["samsungexynos8895"] = "Samsung Exynos 9 Octa 8895"

            map["samsungexynos8890"] = "Samsung Exynos 8 Octa 8890"

            map["samsungexynos9610"] = "Samsung Exynos 7 Octa 9610"
            map["samsungexynos7885"] = "Samsung Exynos 7 Octa 7885"
            map["samsungexynos7880"] = "Samsung Exynos 7 Octa 7880"
            map["samsungexynos7870"] = "Samsung Exynos 7 Octa 7870"
            map["samsungexynos7580"] = "Samsung Exynos 7 Octa 7580"
            map["samsungexynos7570"] = "Samsung Exynos 7 Quad 7570"
            map["samsungexynos7420"] = "Samsung Exynos 7 Octa 7420"
            map["samsungexynos5433"] = "Samsung Exynos 7 Octa 5433"

            map["samsungexynos7872"] = "Samsung Exynos 5 Hexa 7872"
            map["samsungexynos5430"] = "Samsung Exynos 5 Octa 5430"
            map["samsungexynos5422"] = "Samsung Exynos 5 Octa 5422"
            map["samsungexynos5420"] = "Samsung Exynos 5 Octa 5420"
            map["samsungexynos5410"] = "Samsung Exynos 5 Octa 5410"

            return map
        }
}

object GamePackages {
    val gamePackagesList = listOf(
            "com.dts.freefireth", "com.supercell.clashroyale", "com.activision.callofduty.shooter", "com.tencent.ig",
            "com.nianticlabs.pokemongo", "com.igg.android.lordsmobile", "com.supercell.brawlstars", "com.critical.strike2",
            "com.herogame.gplay.hopelessland", "com.junesoftware.maskgun", "com.titan.cd.gb", "com.azurgames.BattleGroundRoyale",
            "com.edkongames.mobs", "com.blayzegames.iosfps", "com.pentagames.crimerevolt", "com.gamedevltd.destinywarfare",
            "com.gameloft.android.ANMP.GloftMVHM", "com.dle.afterpulse", "com.roblox.client", "com.supercell.clashofclans",
            "com.king.candycrushsaga", "com.mobile.legends", "com.mojang.minecraftpe", "com.mojang.minecrafttrialpe",
            "com.mojang.minecraftearth", "com.kabam.marvelbattle", "com.king.candycrushsodasaga", "com.funplus.kingofavalon",
            "com.miniclip.eightballpool", "jp.konami.duellinks", "com.bandainamcoent.dblegends_ww", "com.more.dayzsurvival.gp",
            "com.nintendo.zaka", "com.nintendo.zara", "com.ea.game.simpsons4_row", "zombie.survival.craft.z", "com.pubg.krmobile",
            "com.plarium.raidlegends", "com.ea.gp.fifamobile", "jp.konami.pesam", "com.firsttouchgames.dls3",
            "com.firsttouchgames.story", "com.firsttouchgames.smp", "com.lilithgame.hgame.gp", "com.ngame.allstar.eu",
            "com.fungames.sniper3d", "com.nexters.herowars", "com.crowdstar.covetHome", "com.bandainamcogames.dbzdokkanww",
            "com.garena.game.fcsac", "com.rockstargames.gtasa", "com.im30.ROE.gp", "com.ea.game.starwarscapital_row",
            "com.axlebolt.standoff2", "com.naturalmotion.customstreetracer2", "com.rovio.baba", "eu.nordeus.topeleven.android",
            "com.bandainamcoent.dblegends_ww", "com.square_enix.android_googleplay.FFBEWW", "com.ea.game.pvz2_row",
            "com.tensquaregames.letsfish2", "com.gameloft.android.ANMP.GloftA9HM", "com.ea.game.nfs14_row", "com.klab.bleach",
            "com.gamedevltd.modernstrike", "com.blizzard.wtcg.hearthstone", "com.ubisoft.dance.JustDance", "com.ea.games.r3_row",
            "com.bigfishgames.cookingcrazegooglef2p", "com.kiloo.subwaysurf", "com.imangi.templerun", "com.imangi.templerun2",
            "com.gameloft.android.ANMP.GloftA8HM", "com.fingersoft.hillclimb", "com.hcg.ctw.gp", "com.fingersoft.hcr2",
            "com.tfgco.games.sports.free.tennis.clash", "com.tencent.iglite", "com.epicgames.fortnite", "com.gamedevltd.wwh",
            "com.criticalforceentertainment.criticalops", "com.rockstargames.gtactw", "com.rockstargames.gtavc", "com.rockstar.gta3",
            "com.rockstargames.gtalcs", "org.ppsspp.ppsspp", "org.ppsspp.ppssppgold", "com.epsxe.ePSXe", "com.explusalpha.Snes9xPlus",
            "com.trueaxis.trueskate", "com.trueaxis.truesurf", "se.illusionlabs.bmx", "com.trueaxis.jetcarstunts2", "com.worms3.app",
            "com.prettysimple.criminalcaseandroid", "com.tencent.tmgp.sskeus", "com.squareenixmontreal.hitmansniperandroid",
            "com.ea.game.easportsufc_row", "com.scottgames.fnaf3", "com.gameloft.android.ANMP.GloftM5HM", "uk.co.yakuto.TableTennisTouch",
            "com.halfbrick.fruitninja", "com.playdead.limbo.full", "com.etermax.preguntados.pro", "com.gameinsight.gobandroid",
            "com.gamestar.perfectpiano", "com.sofeh.android.musicstudio3", "com.robtopx.geometryjump", "com.dogbytegames.deadventure",
            "com.dogbytegames.zombiesafari", "com.netmarble.nanagb", "com.netmarble.kofg", "com.blayzegames.newfps",
            "com.miHoYo.GenshinImpact"
    )
}
