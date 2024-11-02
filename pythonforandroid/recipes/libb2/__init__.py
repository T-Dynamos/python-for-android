from pythonforandroid.recipe import Recipe
from pythonforandroid.toolchain import current_directory, shprint, warning
from os.path import join
from multiprocessing import cpu_count
import sh


class Libb2Recipe(Recipe):
    version = '0.98.1'
    url = 'https://github.com/BLAKE2/libb2/releases/download/v{version}/libb2-{version}.tar.gz'
    built_libraries = {'libb2.so': './src/.libs/', "libomp.so": "./src/.libs"}

    def build_arch(self, arch):
        # TODO: this build fails for x86_64 and x86
        # checking whether mmx is supported... /home/tdynamos/p4acache/build/other_builds/libb2/x86_64__ndk_target_24/libb2/configure: line 13165: 0xunknown: value too great for base (error token is "0xunknown")
        if arch.arch in ["x86_64", "x86"]:
            warning(f"libb2 build disabled for {arch.arch}")
            self.built_libraries = {}
            return

        with current_directory(self.get_build_dir(arch.arch)):
            env = self.get_recipe_env(arch)
            flags = [
                '--host=' + arch.command_prefix,
            ]
            configure = sh.Command('./configure')
            shprint(configure, *flags, _env=env)
            shprint(sh.make, "-j",  str(cpu_count()), _env=env)
            arch_ = {"armeabi-v7a":"arm", "arm64-v8a": "aarch64", "x86_64":"x86_64", "x86":"i386"}[arch.arch]
            # also requires libomp.so
            shprint(
                sh.cp, 
                join(self.ctx.ndk.llvm_prebuilt_dir, f"lib64/clang/14.0.6/lib/linux/{arch_}/libomp.so"),
                "./src/.libs"
            )


recipe = Libb2Recipe()
