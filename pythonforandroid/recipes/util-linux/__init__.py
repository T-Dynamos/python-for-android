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
            ]
            configure = sh.Command('./configure', "--without-systemd")
            shprint(configure, *flags, _env=env)

            # --without-systemd option seems not to work, why?
            shprint(sh.sed, "-i", "/#define USE_SYSTEMD 1/d", "config.h")
            shprint(sh.sed, "-i", "/#define HAVE_LIBSYSTEMD 1/d", "config.h")
            shprint(sh.sed, "-i", "s/SYSTEMD_LIBS = -lsystemd/SYSTEMD_LIBS = /", "Makefile")
            
            # build only self.utils
            shprint(sh.make, "-j", str(cpu_count()), *self.utils, _env=env)

recipe = UTIL_LINUXRecipe()
