plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

dependencies {
    implementation("com.github.GTNewHorizons:GT5-Unofficial:5.09.51.476")

    // ❌ 不要写 implementation(project(":Torcherino-GTNH"))
    // ✅ 改为使用 Torcherino 的实际 Maven 坐标
//    implementation("com.github.czqwq:Torcherino:1.2.0-GTNH")
}


// 👇 新增：repositories 块 👇
repositories {
    maven {
        name = "glee8e maven"
        url = uri("https://maven.glease.net/repos/releases/")
    }
}
