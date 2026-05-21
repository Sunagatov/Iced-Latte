@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "common :: *",
                "filestorage :: api",
                "filestorage :: aws",
                "filestorage :: exception"
        }
)
package com.zufar.icedlatte.user;
