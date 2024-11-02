from pythonforandroid.recipe import Recipe
from pythonforandroid.toolchain import current_directory, shprint
from multiprocessing import cpu_count
import sh


class UTIL_LINUXRecipe(Recipe):
    version = '2.40.2'
    url = 'https://mirrors.edge.kernel.org/pub/linux/utils/util-linux/v2.40/util-linux-{version}.tar.xz'
    depends = ["libpthread"]
    built_libraries = {'libuuid.so': './.libs/'}
    utils = ["uuidd"] # enable more utils as per requirements

    def get_recipe_env(self, arch, **kwargs):
        env = super().get_recipe_env(arch, **kwargs)
        if arch.arch in ["x86_64", "arm64_v8a"]:
            env["ac_cv_func_prlimit"] = "yes"
        return env

    def build_arch(self, arch):
        with current_directory(self.get_build_dir(arch.arch)):
            env = self.get_recipe_env(arch)
            flags = [
                '--host=' + arch.command_prefix,
                '--without-systemd',
            ]
            
            if arch.arch in ["armeabi-v7a", "x86"]:
                # Fun fact: Android 32 bit devices won't work in year 2038
                flags.append("--disable-year2038")

            configure = sh.Command('./configure')
            shprint(configure, *flags, _env=env)            
            shprint(sh.make, "-j", str(cpu_count()), *self.utils, _env=env)

recipe = UTIL_LINUXRecipe()
