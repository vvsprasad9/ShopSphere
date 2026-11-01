import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Layers, Plus, Edit2, Trash2, ChevronRight, Folder } from 'lucide-react';
import { categoryApi } from '../../api/categoryApi';
import { useToastStore } from '../../store/useToastStore';
import ConfirmModal from '../../components/common/ConfirmModal';

export default function AdminCategoriesPage() {
  const queryClient = useQueryClient();
  const { success, error } = useToastStore();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [deleteTargetId, setDeleteTargetId] = useState(null);

  const [formData, setFormData] = useState({
    name: '',
    slug: '',
    description: '',
    parentId: '',
    active: true,
  });

  // Fetch full category hierarchy tree
  const { data: treeData, isLoading } = useQuery({
    queryKey: ['adminCategoryTree'],
    queryFn: () => categoryApi.getCategoryHierarchy(),
  });

  // Fetch flat categories for parent dropdown
  const { data: flatCategories } = useQuery({
    queryKey: ['adminCategoriesAll'],
    queryFn: () => categoryApi.getAllCategories(false),
  });

  const categories = treeData || [];
  const parentOptions = flatCategories || [];

  // Create Category Mutation
  const createCategoryMutation = useMutation({
    mutationFn: (data) => categoryApi.createCategory(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminCategoryTree'] });
      queryClient.invalidateQueries({ queryKey: ['adminCategoriesAll'] });
      setModalOpen(false);
      resetForm();
      success('Category created successfully', 'Success');
    },
    onError: (err) => {
      error(err.message || 'Could not create category', 'Error');
    },
  });

  // Update Category Mutation
  const updateCategoryMutation = useMutation({
    mutationFn: ({ id, data }) => categoryApi.updateCategory(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminCategoryTree'] });
      queryClient.invalidateQueries({ queryKey: ['adminCategoriesAll'] });
      setModalOpen(false);
      resetForm();
      success('Category updated successfully', 'Success');
    },
    onError: (err) => {
      error(err.message || 'Could not update category', 'Error');
    },
  });

  // Delete Category Mutation
  const deleteCategoryMutation = useMutation({
    mutationFn: (id) => categoryApi.deleteCategory(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminCategoryTree'] });
      queryClient.invalidateQueries({ queryKey: ['adminCategoriesAll'] });
      setDeleteTargetId(null);
      success('Category deleted successfully', 'Deleted');
    },
    onError: (err) => {
      error(err.message || 'Could not delete category', 'Error');
    },
  });

  const resetForm = () => {
    setEditingCategory(null);
    setFormData({
      name: '',
      slug: '',
      description: '',
      parentId: '',
      active: true,
    });
  };

  const handleOpenEdit = (cat) => {
    setEditingCategory(cat);
    setFormData({
      name: cat.name,
      slug: cat.slug || '',
      description: cat.description || '',
      parentId: cat.parentId || '',
      active: cat.active ?? true,
    });
    setModalOpen(true);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const payload = {
      name: formData.name.trim(),
      slug: formData.slug.trim() || null,
      description: formData.description.trim() || null,
      parentId: formData.parentId ? Number(formData.parentId) : null,
      active: formData.active,
    };

    if (editingCategory) {
      updateCategoryMutation.mutate({ id: editingCategory.id, data: payload });
    } else {
      createCategoryMutation.mutate(payload);
    }
  };

  // Recursive tree renderer
  const renderCategoryNode = (node, depth = 0) => {
    const hasChildren = node.subCategories && node.subCategories.length > 0;

    return (
      <div key={node.id} className="space-y-2">
        <div
          className={`flex items-center justify-between p-3.5 rounded-2xl border transition-all ${
            depth === 0
              ? 'bg-white border-slate-200 shadow-sm'
              : 'bg-slate-50/70 border-slate-100 ml-6'
          }`}
        >
          <div className="flex items-center space-x-3">
            <div
              className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold text-xs ${
                depth === 0 ? 'bg-purple-100 text-purple-700' : 'bg-slate-200 text-slate-600'
              }`}
            >
              <Folder className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-slate-900 text-xs">{node.name}</span>
                <span className="text-[10px] text-slate-400 font-mono">/{node.slug}</span>
              </div>
              {node.description && (
                <p className="text-[11px] text-slate-500 mt-0.5">{node.description}</p>
              )}
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <span
              className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                node.active
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : 'bg-slate-100 text-slate-500'
              }`}
            >
              {node.active ? 'Active' : 'Inactive'}
            </span>

            <button
              type="button"
              onClick={() => handleOpenEdit(node)}
              className="p-1.5 text-slate-400 hover:text-slate-800 rounded-lg hover:bg-slate-100"
              title="Edit Category"
              aria-label="Edit Category"
            >
              <Edit2 className="w-3.5 h-3.5" />
            </button>
            <button
              type="button"
              onClick={() => setDeleteTargetId(node.id)}
              className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50"
              title="Delete Category"
              aria-label="Delete Category"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {hasChildren && (
          <div className="space-y-2">
            {node.subCategories.map((sub) => renderCategoryNode(sub, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="space-y-6 pb-16">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900">Category Hierarchy</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Organize catalog navigation tree and parent-child taxonomy
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            resetForm();
            setModalOpen(true);
          }}
          className="px-4 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white text-xs font-bold transition-colors flex items-center space-x-1.5 shadow-sm"
        >
          <Plus className="w-4 h-4" />
          <span>Add Category</span>
        </button>
      </div>

      {isLoading ? (
        <div className="space-y-3 animate-pulse">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-16 bg-slate-200 rounded-2xl"></div>
          ))}
        </div>
      ) : categories.length === 0 ? (
        <div className="bg-white rounded-3xl border border-slate-200 p-12 text-center shadow-sm space-y-3">
          <Layers className="w-10 h-10 text-slate-400 mx-auto" />
          <h3 className="text-base font-bold text-slate-800">No categories created yet</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            Create your first root category to start organizing your product catalog.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {categories.map((rootNode) => renderCategoryNode(rootNode, 0))}
        </div>
      )}

      {/* Add / Edit Category Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-100 space-y-6">
            <h3 className="text-lg font-bold text-slate-900">
              {editingCategory ? 'Edit Category' : 'Create New Category'}
            </h3>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Category Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Footwear"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  SEO Slug (Optional - auto-generated if left blank)
                </label>
                <input
                  type="text"
                  placeholder="e.g. footwear"
                  value={formData.slug}
                  onChange={(e) => setFormData({ ...formData, slug: e.target.value })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Parent Category (Optional for sub-categories)
                </label>
                <select
                  value={formData.parentId}
                  onChange={(e) => setFormData({ ...formData, parentId: e.target.value })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium"
                >
                  <option value="">None (Top-level Category)</option>
                  {parentOptions
                    .filter((c) => !editingCategory || c.id !== editingCategory.id)
                    .map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Description
                </label>
                <textarea
                  rows={2}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium resize-none"
                />
              </div>

              <label className="flex items-center space-x-2 text-xs font-semibold text-slate-700 cursor-pointer pt-1">
                <input
                  type="checkbox"
                  checked={formData.active}
                  onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                  className="w-4 h-4 text-purple-600 rounded"
                />
                <span>Active & Visible in Navigation</span>
              </label>

              <div className="flex items-center justify-end space-x-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createCategoryMutation.isPending || updateCategoryMutation.isPending}
                  className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-purple-600 hover:bg-purple-700 shadow-sm"
                >
                  {editingCategory ? 'Update Category' : 'Create Category'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={!!deleteTargetId}
        title="Delete Category"
        message="Are you sure you want to delete this category? Sub-categories and products assigned to this category must be reassigned first."
        confirmText="Confirm Delete"
        confirmVariant="danger"
        isLoading={deleteCategoryMutation.isPending}
        onConfirm={() => deleteCategoryMutation.mutate(deleteTargetId)}
        onClose={() => setDeleteTargetId(null)}
      />
    </div>
  );
}
