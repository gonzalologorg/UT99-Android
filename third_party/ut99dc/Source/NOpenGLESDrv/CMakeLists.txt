project(NOpenGLESDrv C CXX)

if(NOT USE_SDL)
  message(FATAL_ERROR "NOpenGLESDrv requires NSDLDrv to function.")
endif()

if(ANDROID)
  set(GLAD_ES_DIR "${CMAKE_SOURCE_DIR}/android_glad_compat")
  set(GLM_DIR "${CMAKE_SOURCE_DIR}/android_glm_compat")
  set(SRC_FILES "NOpenGLESDrv.cpp" "${GLAD_ES_DIR}/glad.c")
else()
  set(GLAD_ES_DIR "${CMAKE_SOURCE_DIR}/../Thirdparty/glad_es")
  set(GLM_DIR "${CMAKE_SOURCE_DIR}/../Thirdparty")
  set(SRC_FILES "NOpenGLESDrv.cpp" "${GLAD_ES_DIR}/glad.c")
endif()

add_library(${PROJECT_NAME} ${LIB_TYPE} ${SRC_FILES})

target_include_directories(${PROJECT_NAME}
  PUBLIC
  ${CMAKE_CURRENT_SOURCE_DIR}
  PRIVATE
  ${GLAD_ES_DIR}
  ${GLM_DIR}
  ${CMAKE_SOURCE_DIR}/../Thirdparty
  ${SDL2_INCLUDE_DIR}
  ${CMAKE_CURRENT_SOURCE_DIR}/../Render/Src
)

target_link_libraries(${PROJECT_NAME} ${SDL2_LIBRARY})
if(ANDROID)
  target_link_libraries(${PROJECT_NAME} GLESv2)
endif()

target_link_libraries(${PROJECT_NAME} Render Engine Core)

target_compile_definitions(${PROJECT_NAME} PRIVATE NOPENGLESDRV_EXPORTS UPACKAGE_NAME=${PROJECT_NAME})
