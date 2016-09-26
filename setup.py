import setuptools
from os import path

if __name__ == "__main__":
    here = path.abspath(path.dirname(__file__))
    with open(path.join(here, 'DESCRIPTION.rst'), encoding='utf-8') as f:
        long_description = f.read()
    setuptools.setup(
        name="watershed",
        version="0.0.1",
        description="Data streams drain into a data reservoir so they can be used later or combined together",
        author="CommerceHub Open Source",
        url="https://github.com/commercehub-oss/watershed",
        long_description=long_description,
        packages=[
            "watershed",
            "pump_client"
        ],
        install_requires=[
            "Boto3",
            "sshtunnel",
            "requests"
        ],
        test_requires=[
            'nose',
            'requests'
        ],
        include_package_data=True,
        classifiers=[
            'Environment :: Console',
            'Intended Audience :: Developers',
            'License :: OSI Approved :: Apache Software License',
            'Operating System :: OS Independent',
            'Programming Language :: Python',
            'Programming Language :: Ruby',
            'Programming Language :: Unix Shell',
            'Topic :: Database',
            'Topic :: Software Development :: Libraries :: Python Modules',
            'Topic :: System :: Archiving',
            'Topic :: System :: Clustering',
            'Topic :: System :: Installation/Setup',
            'Topic :: System :: Systems Administration',
            'Topic :: Utilities'
        ],
        zip_safe=False,
    )