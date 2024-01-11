/*

    Cordova Text-to-Speech Plugin
    https://github.com/vilic/cordova-plugin-tts

    by VILIC VANE
    https://github.com/vilic

    MIT License

*/

exports.speak = function (text) {
    return new Promise(function (resolve, reject) {
        var options = {};

        if (typeof text == "string") {
            options.text = text;
        } else {
            options = text;
        }

        cordova.exec(resolve, reject, "TTS", "speak", [options]);
    });
};

exports.stop = function (reason) {
    return new Promise(function (resolve, reject) {
        const options = {};

        console.log("reason", reason);

        if (reason) {
            options["stopReason"] = reason;
        }

        cordova.exec(resolve, reject, "TTS", "stop", [options]);
    });
};

exports.checkLanguage = function () {
    return new Promise(function (resolve, reject) {
        cordova.exec(resolve, reject, "TTS", "checkLanguage", []);
    });
};

exports.getVoices = function () {
    return new Promise(function (resolve, reject) {
        cordova.exec(resolve, reject, "TTS", "getVoices", []);
    });
};

exports.openInstallTts = function () {
    return new Promise(function (resolve, reject) {
        cordova.exec(resolve, reject, "TTS", "openInstallTts", []);
    });
};

exports.registerSynthesisCallback = () => {
    return new Promise((resolve, reject) => {
        cordova.exec(resolve, reject, "TTS", "registerSynthesisCallback", []);
    });
};

exports.registerRangeStartCallback = () => {
    cordova.exec(
        function (data) {
            const rangeStartEvent = new CustomEvent("onRangeStartCallback", { detail: data });
            document.dispatchEvent(rangeStartEvent);
        },
        null,
        "TTS",
        "registerRangeStartCallback",
        []
    );
};

exports.registerStopCallback = () => {
    cordova.exec(
        function (data) {
            const stopEvent = new CustomEvent("onTtsStop", { detail: data });
            document.dispatchEvent(stopEvent);
        },
        null,
        "TTS",
        "registerStopCallback",
        []
    );
};
