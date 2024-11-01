import zipfile
import json
import os
import subprocess


class ArtifactName:

    platform = "android"
    
    def __init__(self, recipe, arch):
        self.recipe = recipe
        self._arch = arch

    @property
    def stage(self):
        return "master"
        result = subprocess.check_output(
            ["git", "branch", "--show-current"],
            stderr=subprocess.PIPE,
            universal_newlines=True,
        )
        return result.strip()

    @property
    def kind(self):
        return "lib"

    @property
    def arch(self):
        return self._arch.arch

    @property
    def native_api_level(self):
        return str(self.recipe.ctx.ndk_api)

    @property
    def file_props(self):
        return [
            self.stage,
            self.kind,
            self.recipe.name,
            self.arch,
            self.platform + self.native_api_level,
            self.recipe.version,
        ]

    @property
    def filename(self):
        return "_".join(self.file_props) + ".zip"


def build_artifact(
    save_path,
    recipe,
    arch,
    lib_dependencies=[],
    files_dependencies=[],
    install_instructions=[],
):
    # Parse artifact name
    artifact_name = ArtifactName(recipe, arch)
    zip_path = os.path.join(save_path, artifact_name.filename)

    # Contents of zip file
    metadata_folder = "metadata/"
    data_folder = "data/"
    prop_file = os.path.join(metadata_folder, "properties.json")
    install_file = os.path.join(metadata_folder, "install.json")

    properties = {
        "stage": artifact_name.stage,
        "name": recipe.name,
        "arch": artifact_name.arch,
        "native_api_level": artifact_name.native_api_level,
        "kind": artifact_name.kind,
        "version": recipe.version,
        "release_version": recipe.version,
        "lib_dependencies": lib_dependencies,
        "files_dependencies": files_dependencies,
    }

    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
        zipf.writestr(metadata_folder, "")
        zipf.writestr(data_folder, "")

        for file_name in lib_dependencies + files_dependencies:
            with open(file_name, "rb") as file:
                file_name_ = os.path.join(data_folder + os.path.basename(file_name))
                zipf.writestr(file_name_, file.read())
                file.close()

        zipf.writestr(prop_file, json.dumps(properties))
        zipf.writestr(install_file, json.dumps(install_instructions))
        zipf.close()
