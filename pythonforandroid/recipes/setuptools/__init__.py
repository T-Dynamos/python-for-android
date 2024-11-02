from pythonforandroid.recipe import PythonRecipe


class SetuptoolsRecipe(PythonRecipe):
    version = '69.2.0'
    url = ''
    call_hostpython_via_targetpython = False
    install_in_hostpython = True
    hostpython_prerequisites = [f"setuptools=={version}"]


recipe = SetuptoolsRecipe()
