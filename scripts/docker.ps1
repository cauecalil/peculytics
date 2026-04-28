param (
    [Parameter(Mandatory = $true)]
    [ValidateSet("dev", "prod", "down", "clean")]
    [string]$Command
)

$ErrorActionPreference = "Stop"

function Run-Command {
    param (
        [Parameter(Mandatory = $true)]
        [string]$CommandLine
    )

    Write-Host "> $CommandLine"

    Invoke-Expression $CommandLine

    if ($LASTEXITCODE -ne 0) {
        throw "Command failed: $CommandLine"
    }
}

switch ($Command) {
    "dev" {
        Push-Location backend
        Run-Command "mvn package -DskipTests"
        Pop-Location

        $env:DOCKER_TARGET = "dev"
        Run-Command "docker compose up --build"
    }

    "prod" {
        $env:DOCKER_BUILDKIT = "1"
        $env:DOCKER_TARGET = "prod"

        Run-Command "docker compose up --build"
    }

    "down" {
        Run-Command "docker compose down"
    }

    "clean" {
        Push-Location backend
        Run-Command "mvn clean"
        Pop-Location

        Run-Command "docker compose down --rmi local --remove-orphans"
    }
}
