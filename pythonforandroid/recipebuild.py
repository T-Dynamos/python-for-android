import sys
from argparse import ArgumentParser

from pythonforandroid.recipe import Recipe
from pythonforandroid.logger import setup_color, info_main, Colo_Fore
from pythonforandroid.build import Context
from pythonforandroid.graph import get_recipe_build_order
from pythonforandroid.util import ensure_dir
from pythonforandroid.bootstraps.empty import bootstrap

class RecipeBuilder:
    def __init__(self):
        setup_color(True)
        self.build_dir = "/home/tdynamos/p4acache/"
        self.init_context()
        self.build_recipes({"kivy"}, {"arm64-v8a"})

    def init_context(self):
        self.ctx = Context()
        self.ctx.setup_dirs(self.build_dir)
        
        self.ctx.ndk_api = 24
        self.ctx.android_api = 24
        
        

    def build_recipes(self, recipes, archs):
        info_main(f"# Requested recipes: {Colo_Fore.BLUE}{recipes}")
        
        self.ctx.prepare_bootstrap(bootstrap)
        # Setup archs
        self.ctx.set_archs(archs)

        recipes = [
            Recipe.get_recipe(recipe, self.ctx) for recipe in get_recipe_build_order(self.ctx, recipes)
        ]
        
        self.ctx.recipe_build_order = recipes

        # for recipe in recipes:
        #     recipe.download_if_necessary()

        for arch in self.ctx.archs:
            info_main('# Building all recipes for arch {}'.format(arch.arch))

            info_main('# Unpacking recipes')
            for recipe in recipes:
                ensure_dir(recipe.get_build_container_dir(arch.arch))
                recipe.prepare_build_dir(arch.arch)

            info_main('# Prebuilding recipes')
            # 2) prebuild packages
            for recipe in recipes:
                info_main('Prebuilding {} for {}'.format(recipe.name, arch.arch))
                recipe.prebuild_arch(arch)
                recipe.apply_patches(arch)

            # 3) build packages
            info_main('# Building recipes')
            for recipe in recipes:
                info_main('Building {} for {}'.format(recipe.name, arch.arch))
                if recipe.should_build(arch):
                    recipe.build_arch(arch)
                else:
                    info('{} said it is already built, skipping'
                         .format(recipe.name))
                recipe.install_libraries(arch)

            # 4) biglink everything
            info_main('# Biglinking object files')
            if not ctx.python_recipe:
                biglink(ctx, arch)
            else:
                warning(
                    "Context's python recipe found, "
                    "skipping biglink (will this work?)"
                )

            # 5) postbuild packages
            info_main('# Postbuilding recipes')
            for recipe in recipes:
                info_main('Postbuilding {} for {}'.format(recipe.name, arch.arch))
                recipe.postbuild_arch(arch)


if __name__ == "__main__":
    RecipeBuilder()
