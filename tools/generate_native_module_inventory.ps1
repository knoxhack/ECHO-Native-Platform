param(
    [string]$Root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,
    [string]$Output = (Join-Path $Root "echo-native-platform\reports\native-module-inventory.json")
)

$ErrorActionPreference = "Stop"

function Convert-ToRelativePath {
    param([string]$Path)
    $resolved = (Resolve-Path -LiteralPath $Path).Path
    $prefix = $Root.TrimEnd('\') + '\'
    if ($resolved.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        return $resolved.Substring($prefix.Length).Replace('\', '/')
    }
    return $resolved.Replace('\', '/')
}

function Get-TextFiles {
    param([string]$Base)
    if (-not (Test-Path -LiteralPath $Base)) {
        return @()
    }
    Get-ChildItem -LiteralPath $Base -Recurse -File |
        Where-Object {
            $_.FullName -notmatch '\\build\\' -and
            $_.FullName -notmatch '\\tmp\\' -and
            $_.Extension -in @(".java", ".json", ".mcmeta", ".toml", ".md", ".txt")
        }
}

function Search-Files {
    param(
        [System.IO.FileInfo[]]$Files,
        [string]$Pattern
    )
    if (-not $Files -or $Files.Count -eq 0) {
        return @()
    }
    $Files |
        Select-String -Pattern $Pattern -List -ErrorAction SilentlyContinue |
        ForEach-Object { Convert-ToRelativePath $_.Path } |
        Sort-Object -Unique
}

function Count-ResourceFolder {
    param(
        [string]$ResourceRoot,
        [string]$Relative
    )
    $path = Join-Path $ResourceRoot $Relative
    if ($path.Contains("*")) {
        return @(Get-ChildItem -Path $path -Recurse -File -ErrorAction SilentlyContinue).Count
    }
    if (-not (Test-Path -LiteralPath $path)) {
        return 0
    }
    return @(Get-ChildItem -LiteralPath $path -Recurse -File -ErrorAction SilentlyContinue).Count
}

function Get-DataSummary {
    param([string]$ResourceRoot)
    if (-not (Test-Path -LiteralPath $ResourceRoot)) {
        return @{
            namespaces = @()
            dataRoots = @()
            assetNamespaces = @()
            worldgen = @{}
            structures = 0
            recipes = 0
            lootTables = 0
            tags = 0
        }
    }

    $dataRoot = Join-Path $ResourceRoot "data"
    $assetRoot = Join-Path $ResourceRoot "assets"
    $namespaces = @()
    $dataRoots = @()
    if (Test-Path -LiteralPath $dataRoot) {
        $namespaces = @(Get-ChildItem -LiteralPath $dataRoot -Directory | ForEach-Object { $_.Name } | Sort-Object -Unique)
        foreach ($namespace in $namespaces) {
            $namespaceRoot = Join-Path $dataRoot $namespace
            foreach ($dir in Get-ChildItem -LiteralPath $namespaceRoot -Directory -ErrorAction SilentlyContinue) {
                $count = @(Get-ChildItem -LiteralPath $dir.FullName -Recurse -File -ErrorAction SilentlyContinue).Count
                $dataRoots += [ordered]@{
                    namespace = $namespace
                    root = $dir.Name
                    files = $count
                }
            }
        }
    }

    $assetNamespaces = @()
    if (Test-Path -LiteralPath $assetRoot) {
        $assetNamespaces = @(Get-ChildItem -LiteralPath $assetRoot -Directory | ForEach-Object { $_.Name } | Sort-Object -Unique)
    }

    $worldgenKinds = @(
        "worldgen/biome",
        "worldgen/configured_feature",
        "worldgen/placed_feature",
        "worldgen/structure",
        "worldgen/structure_set",
        "worldgen/template_pool",
        "worldgen/processor_list",
        "worldgen/noise_settings",
        "worldgen/world_preset",
        "dimension",
        "dimension_type",
        "structures"
    )
    $worldgen = [ordered]@{}
    foreach ($kind in $worldgenKinds) {
        $total = 0
        foreach ($namespace in $namespaces) {
            $total += Count-ResourceFolder $ResourceRoot ("data\$namespace\$kind")
        }
        $worldgen[$kind.Replace("/", ".")] = $total
    }

    return [ordered]@{
        namespaces = $namespaces
        dataRoots = @($dataRoots | Sort-Object namespace, root)
        assetNamespaces = $assetNamespaces
        worldgen = $worldgen
        structures = ($worldgen["structures"])
        recipes = (Count-ResourceFolder $ResourceRoot "data\*\recipe")
        lootTables = (Count-ResourceFolder $ResourceRoot "data\*\loot_table")
        tags = (Count-ResourceFolder $ResourceRoot "data\*\tags")
    }
}

function Get-KeyNames {
    param([System.IO.FileInfo[]]$Files)
    if (-not $Files -or $Files.Count -eq 0) {
        return @()
    }
    $matches = $Files |
        Select-String -Pattern "GLFW_KEY_[A-Z0-9_]+" -AllMatches -ErrorAction SilentlyContinue
    return @($matches.Matches.Value | Sort-Object -Unique)
}

function Get-ModuleInventory {
    param(
        [string]$Name,
        [string]$Base
    )

    $javaRoot = Join-Path $Base "src\main\java"
    $resourceRoot = Join-Path $Base "src\main\resources"
    $files = @(Get-TextFiles $Base)
    $javaFiles = @($files | Where-Object { $_.Extension -eq ".java" })
    $resourceFiles = @()
    if (Test-Path -LiteralPath $resourceRoot) {
        $resourceFiles = @(Get-ChildItem -LiteralPath $resourceRoot -Recurse -File -ErrorAction SilentlyContinue)
    }

    $keyFiles = @(Search-Files $javaFiles "KeyMapping|GLFW_KEY_")
    $nativeModules = @()
    if (Test-Path -LiteralPath $javaRoot) {
        $nativeModules = @(Get-ChildItem -LiteralPath $javaRoot -Recurse -Filter "*NativeModule.java" -File -ErrorAction SilentlyContinue |
            ForEach-Object { Convert-ToRelativePath $_.FullName } |
            Sort-Object)
    }

    return [ordered]@{
        name = $Name
        root = (Convert-ToRelativePath $Base)
        nativeModules = $nativeModules
        nativeHookFiles = @(Search-Files $javaFiles "NativeHost|NativeBridge|NativeBootstrap|EchoNative|native platform|native route|NativeModule")
        keyMappingFiles = $keyFiles
        glfwKeys = @(Get-KeyNames $javaFiles)
        screenFiles = @($javaFiles |
            Where-Object { $_.Name -match "Screen|Screens|Overlay|Hud|HUD" -or $_.FullName -match "\\client\\screen\\" } |
            ForEach-Object { Convert-ToRelativePath $_.FullName } |
            Sort-Object -Unique)
        menuFiles = @($javaFiles |
            Where-Object { $_.Name -match "Menu|MenuType|Container" } |
            ForEach-Object { Convert-ToRelativePath $_.FullName } |
            Sort-Object -Unique)
        packetFiles = @(Search-Files $javaFiles "CustomPacketPayload|StreamCodec|PacketCodec|SimpleChannel|serverbound|clientbound|Payload")
        blockFiles = @(Search-Files $javaFiles "Registries\.BLOCK|DeferredRegister<\s*Block|RegisterEvent.*BLOCK|BlockBehaviour|extends Block")
        itemFiles = @(Search-Files $javaFiles "Registries\.ITEM|DeferredRegister<\s*Item|RegisterEvent.*ITEM|extends Item|Item\.Properties")
        entityFiles = @(Search-Files $javaFiles "Registries\.ENTITY_TYPE|DeferredRegister<\s*EntityType|extends .*Entity|MobCategory|EntityType")
        blockEntityFiles = @(Search-Files $javaFiles "BLOCK_ENTITY_TYPE|BlockEntityType|extends BlockEntity")
        data = (Get-DataSummary $resourceRoot)
        sourceCounts = [ordered]@{
            java = $javaFiles.Count
            resources = $resourceFiles.Count
        }
    }
}

$addonRoot = Join-Path $Root "addons"
$addons = @()
if (Test-Path -LiteralPath $addonRoot) {
    $addons = @(Get-ChildItem -LiteralPath $addonRoot -Directory | Sort-Object Name | ForEach-Object {
        Get-ModuleInventory -Name $_.Name -Base $_.FullName
    })
}

$all = @($addons)
$summary = [ordered]@{
    generatedAt = (Get-Date).ToString("s")
    root = $Root.Replace('\', '/')
    addonCount = $addons.Count
    moduleCount = $all.Count
    nativeModuleCount = @($all | Where-Object { $_.nativeModules.Count -gt 0 }).Count
    keyMappedModuleCount = @($all | Where-Object { $_.keyMappingFiles.Count -gt 0 }).Count
    screenModuleCount = @($all | Where-Object { $_.screenFiles.Count -gt 0 }).Count
    packetModuleCount = @($all | Where-Object { $_.packetFiles.Count -gt 0 }).Count
    blockModuleCount = @($all | Where-Object { $_.blockFiles.Count -gt 0 }).Count
    itemModuleCount = @($all | Where-Object { $_.itemFiles.Count -gt 0 }).Count
    entityModuleCount = @($all | Where-Object { $_.entityFiles.Count -gt 0 }).Count
}

$inventory = [ordered]@{
    summary = $summary
    modules = $all
}

$outputDir = Split-Path -Parent $Output
if (-not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}
$inventory | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $Output -Encoding UTF8
Write-Host "Wrote $Output"
Write-Host ($summary | ConvertTo-Json -Depth 4)
