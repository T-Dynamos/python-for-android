from os.path import join
import shutil

from pythonforandroid.recipe import NDKRecipe
from pythonforandroid.util import ensure_dir


class Sqlite3Recipe(NDKRecipe):
    version = '3.46.0'
    # Don't forget to change the URL when changing the version
    url = 'https://sqlite.org/2024/sqlite-amalgamation-3460000.zip'
    generated_libraries = ['sqlite3']
    built_libraries = {"libsqlite3.so": "."}

    def should_build(self, arch):
        self.built_libraries["libsqlite3.so"] = join(self.get_build_dir(arch.arch), 'libs', arch.arch)
        return not self.has_libs(arch, 'libsqlite3.so')

    def prebuild_arch(self, arch):
        super().prebuild_arch(arch)
        # Copy the Android make file
        ensure_dir(join(self.get_build_dir(arch.arch), 'jni'))
        shutil.copyfile(join(self.get_recipe_dir(), 'Android.mk'),
                        join(self.get_build_dir(arch.arch), 'jni/Android.mk'))

    def build_arch(self, arch, *extra_args):
        super().build_arch(arch)
        # Copy the shared library
        shutil.copyfile(join(self.get_build_dir(arch.arch), 'libs', arch.arch, 'libsqlite3.so'),
                        join(self.ctx.get_libs_dir(arch.arch), 'libsqlite3.so'))

    def get_recipe_env(self, arch):
        env = super().get_recipe_env(arch)
        env['NDK_PROJECT_PATH'] = self.get_build_dir(arch.arch)
        return env


recipe = Sqlite3Recipe()
