# Merges archives into one static library. Invoked by jni/CMakeLists.txt.
#
# macOS' ar cannot merge, so libtool is used there. Elsewhere ar's MRI
# addlib support merges the archives without losing duplicate member names.

if(NOT OUTPUT OR NOT INPUTS OR NOT AR)
  message(FATAL_ERROR "merge_archives.cmake requires OUTPUT, INPUTS and AR")
endif()

string(REPLACE "|" ";" INPUTS "${INPUTS}")

if(IS_APPLE)
  execute_process(
    COMMAND /usr/bin/libtool -static -o "${OUTPUT}" ${INPUTS}
    RESULT_VARIABLE result
    OUTPUT_VARIABLE output
    ERROR_VARIABLE error
  )
  if(NOT result EQUAL 0)
    message(FATAL_ERROR "libtool failed (${result}): ${output}\n${error}")
  endif()
  return()
endif()

file(REMOVE_RECURSE "${WORK_DIR}")
file(MAKE_DIRECTORY "${WORK_DIR}")
file(REMOVE "${OUTPUT}")

# FFmpeg archives contain several members with the same basename (cabac.o is
# present in h264, vvc and other codec directories). Extracting them into one
# directory would silently overwrite all but one, so merge through ar's MRI
# script support, which keeps every member.
set(MRI "")
string(APPEND MRI "create ${OUTPUT}\n")
foreach(archive IN LISTS INPUTS)
  string(APPEND MRI "addlib ${archive}\n")
endforeach()
string(APPEND MRI "save\nend\n")
file(WRITE "${WORK_DIR}/merge.mri" "${MRI}")
execute_process(
  COMMAND "${AR}" -M
  INPUT_FILE "${WORK_DIR}/merge.mri"
  RESULT_VARIABLE result
  ERROR_VARIABLE error
)
if(NOT result EQUAL 0)
  message(FATAL_ERROR "ar -M failed (${result}): ${error}")
endif()
