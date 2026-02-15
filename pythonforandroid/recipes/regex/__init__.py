from pythonforandroid.recipe import PyProjectRecipe


class RegexRecipe(PyProjectRecipe):
    name = "regex"
    version = "2026.1.15"
    url = "https://github.com/mrabarnett/mrab-regex/archive/refs/tags/{version}.tar.gz"


recipe = RegexRecipe()
