from pythonforandroid.recipe import PythonRecipe, Recipe
from os.path import join

class PYCURLRecipe(PythonRecipe):
    version = "7_45_3"
    url = "https://github.com/pycurl/pycurl/archive/refs/tags/REL_{version}.tar.gz"
    depends = ["libcurl", "setuptools"]
    call_hostpython_via_targetpython = False

    def get_recipe_env(self, arch, **kwargs):
        env = super().get_recipe_env(arch, **kwargs)
        env["CFLAGS"] += " -I" + join(Recipe.get_recipe("libcurl",self.ctx).get_build_dir(arch), "include")
        openssl_recipe = Recipe.get_recipe("openssl",self.ctx)
        env["PYCURL_SSL_LIBRARY"] = "openssl"
        env["CFLAGS"] += openssl_recipe.include_flags(arch)
        env["LDFLAGS"] += openssl_recipe.link_dirs_flags(arch)
        return env



recipe = PYCURLRecipe()
