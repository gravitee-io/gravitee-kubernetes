## [3.3.1](https://github.com/gravitee-io/gravitee-kubernetes/compare/3.3.0...3.3.1) (2024-06-12)


### Bug Fixes

* skip processing up-to-date config maps on reconnect ([fa7e9e6](https://github.com/gravitee-io/gravitee-kubernetes/commit/fa7e9e6f26e81e47de61ff4f5568b30390046c9b))

# [3.3.0](https://github.com/gravitee-io/gravitee-kubernetes/compare/3.2.1...3.3.0) (2024-05-17)


### Bug Fixes

* **deps:** update dependency io.gravitee:gravitee-bom to v6.0.47 ([cb4293c](https://github.com/gravitee-io/gravitee-kubernetes/commit/cb4293c4cff80b73d3590132744ad38bb8fc7add))


### Features

* add api definition v4 group version kind ([281f169](https://github.com/gravitee-io/gravitee-kubernetes/commit/281f169f19df0c4b3f596d3a81ba3d9ff0146ddb))

## [3.2.1](https://github.com/gravitee-io/gravitee-kubernetes/compare/3.2.0...3.2.1) (2024-05-16)


### Bug Fixes

* ensure custom resources names are rfc 1123 compliant ([f9deec5](https://github.com/gravitee-io/gravitee-kubernetes/commit/f9deec592acbbe6383892c3f0899b317ee8c7cf7))

# [3.2.0](https://github.com/gravitee-io/gravitee-kubernetes/compare/3.1.0...3.2.0) (2024-04-09)


### Features

* repeat watch on websocket disconnect ([b8e7a3d](https://github.com/gravitee-io/gravitee-kubernetes/commit/b8e7a3de7cfec544538efb00dacc894fa0b80bd8))

# [3.1.0](https://github.com/gravitee-io/gravitee-kubernetes/compare/3.0.2...3.1.0) (2023-12-12)


### Features

* create watchable resources using the client ([16f05ef](https://github.com/gravitee-io/gravitee-kubernetes/commit/16f05ef6b9ddff2921bc8d66184bdba08ac363f1))

## [3.0.2](https://github.com/gravitee-io/gravitee-kubernetes/compare/3.0.1...3.0.2) (2023-11-17)


### Bug Fixes

* take pem files into account when reading kubeconfig ([00a771a](https://github.com/gravitee-io/gravitee-kubernetes/commit/00a771a8b1efdd8dc21219cd58924a36b6e97a1b))

## [3.0.1](https://github.com/gravitee-io/gravitee-kubernetes/compare/3.0.0...3.0.1) (2023-10-24)


### Bug Fixes

* add missing header ([50dbd2a](https://github.com/gravitee-io/gravitee-kubernetes/commit/50dbd2a9b63ad2f62e207d2c997b27d72196e846))
* add ownerReferences to the ObjectMeta ([5ee8de7](https://github.com/gravitee-io/gravitee-kubernetes/commit/5ee8de77bb73e60c2b9a28eb50df1abfe1c29c5b))
* **deps:** update dependency io.gravitee:gravitee-bom to v6.0.16 ([6794b6d](https://github.com/gravitee-io/gravitee-kubernetes/commit/6794b6dbbc42af887c0aac7197939d6fec986c9e))
* **deps:** update dependency io.gravitee:gravitee-parent to v22.0.10 ([4acacb0](https://github.com/gravitee-io/gravitee-kubernetes/commit/4acacb0d332d917874ebecc5b73ab879c0be3218))
* **deps:** update fabric8-kubernetes-client monorepo to v4.13.3 ([55db6a3](https://github.com/gravitee-io/gravitee-kubernetes/commit/55db6a37b4ad96caa99a0b225ec4be08c022cd9f))

# [3.0.0](https://github.com/gravitee-io/gravitee-kubernetes/compare/2.1.1...3.0.0) (2023-10-03)


### Bug Fixes

* clear as much Sonar issue as possible ([e41825e](https://github.com/gravitee-io/gravitee-kubernetes/commit/e41825e1767e5b28891da6ee69100912aff2d4e6))
* failing test due to static usage ([9acb555](https://github.com/gravitee-io/gravitee-kubernetes/commit/9acb5557c4113671990b5eae372cf13e0c96f2ed))


### chore

* configure pom with latest deps ([c3be0dd](https://github.com/gravitee-io/gravitee-kubernetes/commit/c3be0dd5f1ce5a6aefb0b6645de04d488b7710fa))


### BREAKING CHANGES

* bump compilation level to 17, upgrade build stack

## [2.1.1](https://github.com/gravitee-io/gravitee-kubernetes/compare/2.1.0...2.1.1) (2023-09-29)


### Bug Fixes

* always connect with mTLS + tests ([48cf7dd](https://github.com/gravitee-io/gravitee-kubernetes/commit/48cf7dd3df69577a342c99aeb91fc8a0eda9aae9))
* get namespace from context file, connect via mTLS ([0d8954e](https://github.com/gravitee-io/gravitee-kubernetes/commit/0d8954eb71737df20238f354484763f5690829bb))
* infer namespace from context file ([425774e](https://github.com/gravitee-io/gravitee-kubernetes/commit/425774ed095fdf1b2bfd3a2787c90ccfb71c2c61))

# [2.1.0](https://github.com/gravitee-io/gravitee-kubernetes/compare/2.0.3...2.1.0) (2023-09-25)


### Bug Fixes

* check override file only is not null or blank ([d7c35b9](https://github.com/gravitee-io/gravitee-kubernetes/commit/d7c35b919760784fcd699cae947040731ee766c7))
* fixes floowing review ([79da848](https://github.com/gravitee-io/gravitee-kubernetes/commit/79da8485311f3264c9d3586d5b48261b604088f3))
* KubernetesConfig.java text typo ([91f88d2](https://github.com/gravitee-io/gravitee-kubernetes/commit/91f88d26caa01a73c88be66b20c225ca38d0b898))


### Features

* allow kube client to use alternate kube config and defaults at creation time. ([#56](https://github.com/gravitee-io/gravitee-kubernetes/issues/56)) ([13957fd](https://github.com/gravitee-io/gravitee-kubernetes/commit/13957fda0d0233f653c657791b48569c1f78c7a5))
* allow kube client to use alternate kube config file and defaults at creation time ([ebc2aba](https://github.com/gravitee-io/gravitee-kubernetes/commit/ebc2aba641b4cf94571afa7f2153559199bed5ff))

## [2.0.3](https://github.com/gravitee-io/gravitee-kubernetes/compare/2.0.2...2.0.3) (2023-05-16)


### Bug Fixes

* maintain only one dedicated vertx instance ([7402ba9](https://github.com/gravitee-io/gravitee-kubernetes/commit/7402ba95a69b367f7af6acf13740939283fbf032))

## [2.0.2](https://github.com/gravitee-io/gravitee-kubernetes/compare/2.0.1...2.0.2) (2023-05-03)


### Bug Fixes

* use dedicated vertx instance in kubernetes client ([0d1d9aa](https://github.com/gravitee-io/gravitee-kubernetes/commit/0d1d9aa1ea428bc7a6cdf0275ce471659074e7dc))

## [2.0.1](https://github.com/gravitee-io/gravitee-kubernetes/compare/2.0.0...2.0.1) (2023-02-17)


### Bug Fixes

* **deps:** update dependency io.gravitee.common:gravitee-common to v1.28.0 ([01a4afd](https://github.com/gravitee-io/gravitee-kubernetes/commit/01a4afd7804f2ebefcdf310c144fbb59fb887b8b))
* update the watch query ([9e30d71](https://github.com/gravitee-io/gravitee-kubernetes/commit/9e30d719b8003bf3529b3101e6ac7da5b5970833))

# [2.0.0](https://github.com/gravitee-io/gravitee-kubernetes/compare/1.0.0...2.0.0) (2022-11-30)


### chore

* **deps:** bump to rxJava3 ([4adad6e](https://github.com/gravitee-io/gravitee-kubernetes/commit/4adad6eccf6509188c608a0c55908af5491cf069))


### BREAKING CHANGES

* **deps:** rxJava3 required

# [2.0.0-alpha.1](https://github.com/gravitee-io/gravitee-kubernetes/compare/1.0.0...2.0.0-alpha.1) (2022-11-08)


### chore

* **deps:** bump to rxJava3 ([4adad6e](https://github.com/gravitee-io/gravitee-kubernetes/commit/4adad6eccf6509188c608a0c55908af5491cf069))


### BREAKING CHANGES

* **deps:** rxJava3 required

# [1.0.0](https://github.com/gravitee-io/gravitee-kubernetes/compare/0.4.0...1.0.0) (2022-11-08)


### Features

* remove Kubernetes - Controller module ([3fb69e6](https://github.com/gravitee-io/gravitee-kubernetes/commit/3fb69e667a647fbddb66518dc6f900d256be527d))


### BREAKING CHANGES

* remove "Kubernetes - Controller" module.
This module is no more used and maintained with last apim version
