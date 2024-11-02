from pythonforandroid.recipe import Recipe
from pythonforandroid.toolchain import current_directory, shprint
import sh


class Libb2Recipe(Recipe):
    version = '0.98.1'
    url = 'https://github.com/BLAKE2/libb2/releases/download/v{version}/libb2-{version}.tar.gz'
    built_libraries = {'libb2.so': './src/.libs/'}

    def build_arch(self, arch):
        with current_directory(self.get_build_dir(arch.arch)):
            env = self.get_recipe_env(arch)
            flags = [
                '--host=' + arch.command_prefix,
            ]
            configure = sh.Command('./configure')
            shprint(configure, *flags, _env=env)
            shprint(sh.make, _env=env)


recipe = Libb2Recipe()
