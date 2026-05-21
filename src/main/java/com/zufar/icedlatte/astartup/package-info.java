@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "filestorage :: api",
                "filestorage :: exception",
                "product :: api",
                "review :: api"
        }
)
package com.zufar.icedlatte.astartup;
