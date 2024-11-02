from distutils.core import setup, Extension
from Cython.Build import cythonize
import os

# Define the library directories
library_dirs = [os.environ['ANDROID_LIBS_DIR']]
lib_dict = {
    'sdl2': ['SDL2', 'SDL2_image', 'SDL2_mixer', 'SDL2_ttf']
}
sdl_libs = lib_dict.get(os.environ['BOOTSTRAP'], ['main'])

# Define the extensions with Cython
modules = [
    Extension('android._android',
              ['android/_android.pyx', 'android/_android_jni.c'],
              libraries=sdl_libs + ['log'],
              library_dirs=library_dirs),
    Extension('android._android_billing',
              ['android/_android_billing.pyx', 'android/_android_billing_jni.c'],
              libraries=['log'],
              library_dirs=library_dirs),
    Extension('android._android_sound',
              ['android/_android_sound.pyx', 'android/_android_sound_jni.c'],
              libraries=['log'],
              library_dirs=library_dirs)
]


# Use cythonize to build the modules
cythonized_modules = cythonize(modules, compiler_directives={'language_level': "3"})

# Setup the package
setup(name='android',
      version='1.0',
      packages=['android'],
      package_dir={'android': 'android'},
      ext_modules=cythonized_modules
      )
