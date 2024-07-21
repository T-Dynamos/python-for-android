import sys
import os
from argparse import ArgumentParser

from pythonforandroid.recipe import Recipe
from pythonforandroid.logger import setup_color, info_main, Colo_Fore, info
from pythonforandroid.build import Context
from pythonforandroid.graph import get_recipe_order_and_bootstrap
from pythonforandroid.util import ensure_dir
from pythonforandroid.bootstraps.empty import bootstrap
from pythonforandroid.distribution import Distribution
from pythonforandroid.androidndk import AndroidNDK


class RecipeBuilder:
    def __init__(self):
        setup_color(True)
        self.build_dir = "/home/tdynamos/p4acache/"
        self.init_context()
        self.build_recipes({"kivy"}, {"arm64-v8a"})

    def init_context(self):
        self.ctx = Context()
        self.ctx.save_prebuilt = True
        self.ctx.setup_dirs(self.build_dir)

        self.ctx.ndk_api = 24
        self.ctx.android_api = 24
        self.ctx.ndk_dir = "/home/tdynamos/.buildozer/android/platform/android-ndk-r25b"

    def build_recipes(self, recipes, archs):
        info_main(f"# Requested recipes: {Colo_Fore.BLUE}{recipes}")

        _recipes, _non_recipes, bootstrap = get_recipe_order_and_bootstrap(
            self.ctx, recipes
        )
        self.ctx.prepare_bootstrap(bootstrap)
        self.ctx.set_archs(archs)
        self.ctx.bootstrap.distribution = Distribution.get_distribution(
            self.ctx, name=bootstrap.name, recipes=recipes, archs=archs,
        )
        self.ctx.ndk = AndroidNDK("/home/tdynamos/.buildozer/android/platform/android-ndk-r25b")
        recipes = [Recipe.get_recipe(recipe, self.ctx) for recipe in _recipes]

        self.ctx.recipe_build_order = recipes
        #for recipe in recipes:
        #    recipe.download_if_necessary()

        for arch in self.ctx.archs:
            info_main("# Building all recipes for arch {}".format(arch.arch))
            #
            # info_main("# Unpacking recipes")
            # for recipe in recipes:
            #     ensure_dir(recipe.get_build_container_dir(arch.arch))
            #     recipe.prepare_build_dir(arch.arch)
            #
            info_main("# Prebuilding recipes")
            # 2) prebuild packages
            for recipe in recipes:
                info_main("Prebuilding {} for {}".format(recipe.name, arch.arch))
                recipe.prebuild_arch(arch)
                recipe.apply_patches(arch)

            info_main("# Building recipes")
            for recipe in recipes:
                info_main("Building {} for {}".format(recipe.name, arch.arch))
                if recipe.should_build(arch):
                    recipe.build_arch(arch)
                else:
                    info("{} said it is already built, skipping".format(recipe.name))
                recipe.install_libraries(arch)

                input()

if __name__ == "__main__":
    RecipeBuilder()
