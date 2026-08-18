# ZyxoKits — Paper 1.21.11

This repository is prepared for GitHub Actions. No PC/Maven installation is required.

## Mobile build
1. Create a GitHub repository and upload all files from this project.
2. Open **Actions**.
3. Select **Build ZyxoKits**.
4. Tap **Run workflow**.
5. After it finishes, open the workflow run and download the artifact **ZyxoKits-1.21.11**.
6. Inside the downloaded ZIP is the compiled `ZyxoKits-1.0.0.jar`.
7. Put that JAR in your Paper 1.21.11 server's `plugins` folder.

The workflow uses Java 21 and Maven and builds on GitHub's servers.
