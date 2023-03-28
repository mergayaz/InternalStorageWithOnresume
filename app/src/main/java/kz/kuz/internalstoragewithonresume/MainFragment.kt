package kz.kuz.internalstoragewithonresume

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// цель - разрешить другому приложению (фотокамере) сохранять файлы в песочнице данного приложения
// это делается с помощью класса FileProvider
// в манифест добавлен блок "provider", в нём FileProvider объявляется как
// экземпляр ContentProvider, связанный с конкретным хранилищем (authority)
// таким образом другим приложениям предоставляется цель для запросов
// также другим приложениям нужно дать разрешение на запись по URI
// это делается посредством атрибута "grantUriPermissions" true в манифесте
// также добавляется ресурс res/xml/files.xml
// в этом ресурсе папка "some_files" объявляется корневым путём для FileProvider
// ссылка на ресурс добавляется в манифест, в "provider" как meta-data
// поскольку в приложении используется камера, то информация об этом также добавляется в манифест
// как uses-feature
class MainFragment : Fragment() {
    private lateinit var photoFile: File
    private lateinit var photoView: ImageView
    private lateinit var rotateButton: Button
    private lateinit var bitmap: Bitmap
    private var REQUEST_PHOTO = 0
    private var backFromCamera = false

    // методы фрагмента должны быть открытыми
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        activity?.setTitle(R.string.toolbar_title)
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val btnTakePicture = view.findViewById<ImageButton>(R.id.take_picture)
        val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // класс MediaStore определяет открытые интерфейсы для работы с аудиовизуальными материалами
        // по умолчанию ACTION_IMAGE_CAPTURE создаёт миниатюру с малым разрешением, упаковывает в
        // объект Intent и возвращает в onActivityResult()
        // чтобы получить изображение в высоком разрешении, нужно сообщить, где должно храниться
        // изображение в файловой системе
        // эта задача решается передачей URI места сохранения файла в MediaStore.EXTRA_OUTPUT
        photoFile = File(context?.filesDir, "file_name.jpg")
        // здесь файл не создаётся, создаётся лишь объект в памяти
        // в синглете вместо getContext() лучше использовать getContext().getApplicationContext()
        val packageManager = activity!!.packageManager
        val canTakePhoto = photoFile != null &&
                captureImage.resolveActivity(packageManager) != null
        // проверяем доступность камеры, при недоступности блокируем кнопку
        btnTakePicture.isEnabled = canTakePhoto
        btnTakePicture.setOnClickListener {
            val uri = FileProvider.getUriForFile(activity!!,
                    "kz.kuz.fragmentapplication.fileprovider",
                    photoFile)
            // getUriForFile преобразует локальный путь к файлу в объект URI, понятный
            // приложению камеры
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            val cameraActivities = activity!!.packageManager
                    .queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            // с помощью PackageManager получаем список приложений, способных делать фото
            for (activity in cameraActivities) {
                getActivity()!!.grantUriPermission(activity.activityInfo.packageName, uri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            // каждому полученному приложению даём право на запись в хранилище
            // данная функциональность разблокирована в манифесте
            startActivityForResult(captureImage, REQUEST_PHOTO)
        }
        photoView = view.findViewById(R.id.photo_view)

        // часть с rotateButton я добавил самостоятельно
        rotateButton = view.findViewById(R.id.rotate_photo)
        rotateButton.setOnClickListener(View.OnClickListener {
            try {
                rotatePhoto()
            } catch (e: IOException) {
            }
        })

        // код ниже необходим для того, чтобы извлекать размеры контейнера photoView после
        // обработки макета (код не завершён)
        // слушатель OnGlobalLayoutListener инициирует событие каждый раз, когда происходит
        // обработка макета
        // дело в том, что при первоначальном изменении размера изображения макет ещё не обработан
        // и у нас нет информации о размере контейнера
        // по этой причине мы уменьшаем размер изображения не до размера контейнера, а до размера
        // экрана
        // с помощью OnGlobalLayoutListener можно инициировать событие после первой обработки макета

//        val observer = photoView.viewTreeObserver
//        observer.addOnGlobalLayoutListener { }
//        updatePhotoView()

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PHOTO) {
            backFromCamera = true
            val uri = FileProvider.getUriForFile(activity!!,
                    "kz.kuz.fragmentapplication.fileprovider", photoFile)
            activity?.revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            // отзываем разрещение на запись в хранилище
            // revokeUriPermission отзывает все разрешения, выданные ранее на запись в указанный URI
            updatePhotoView()
        }
    }

    override fun onResume() {
        super.onResume()
        if (backFromCamera) {
            Toast.makeText(context, "Back from camera!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePhotoView() {
        if (photoFile == null || !photoFile.exists()) {
            photoView.setImageDrawable(null)
            rotateButton.isEnabled = false
        } else {
            bitmap = PictureUtils.getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
            rotateButton.isEnabled = true
        }
    }

    @Throws(IOException::class)
    private fun rotatePhoto() {
        if (photoFile == null || !photoFile.exists()) {
        } else {
            val matrix = Matrix()
            matrix.postRotate(90f)
            val initialBitmap = BitmapFactory.decodeFile(photoFile.path)
            val rotatedBitmap = Bitmap.createBitmap(initialBitmap, 0, 0,
                    initialBitmap.width, initialBitmap.height, matrix, true)
            val out = FileOutputStream(photoFile)
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
            bitmap = PictureUtils.getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        }
    }
}